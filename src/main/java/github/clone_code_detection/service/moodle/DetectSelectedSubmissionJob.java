package github.clone_code_detection.service.moodle;

import github.clone_code_detection.entity.authenication.UserImpl;
import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.entity.highlight.document.ReportSourceDocument;
import github.clone_code_detection.entity.highlight.document.SimilarityReport;
import github.clone_code_detection.entity.highlight.request.SimilarityDetectRequest;
import github.clone_code_detection.entity.moodle.*;
import github.clone_code_detection.entity.query.QueryInstruction;
import github.clone_code_detection.exceptions.UnsupportedLanguage;
import github.clone_code_detection.exceptions.moodle.MoodleSubmissionException;
import github.clone_code_detection.repo.RepoElasticsearchDelete;
import github.clone_code_detection.repo.RepoSimilarityReport;
import github.clone_code_detection.repo.RepoSubmission;
import github.clone_code_detection.service.query.ServiceQuery;
import github.clone_code_detection.util.FileSystemUtil;
import github.clone_code_detection.util.LanguageUtil;
import github.clone_code_detection.util.ZipUtil;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.modelmapper.internal.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Builder

@NoArgsConstructor(force = true)
@AllArgsConstructor
public class DetectSelectedSubmissionJob implements Runnable {
    private static final LanguageUtil languageUtil = LanguageUtil.getInstance();
    @NotNull
    final UserImpl user;
    final List<Long> submissionIds;
    @NotNull
    private final RepoSimilarityReport repoSimilarityReport;
    @NotNull
    private final RepoSubmission repoSubmission;
    @NotNull
    private final ServiceQuery serviceQuery;
    @NotNull
    private final RepoElasticsearchDelete repoElasticsearchDelete;
    @NotNull
    private final RestTemplate moodleClient;

    private QueryInstruction queryInstruction;

    private Map<String, List<RelationWithOwner>> classifySubmissions(List<Submission> submissions) {
        Map<String, List<RelationWithOwner>> mapRelationsByNeed = new HashMap<>();
        submissions.forEach(submission -> submission.getRelations().forEach(relation -> {
            RelationWithOwner relationWithOwner = RelationWithOwner.builder()
                                                                   .owner(submission.getOwner())
                                                                   .relation(relation)
                                                                   .build();
            // If session is null then this relation need to add
            // If submission relation has session id = null then add new
            if (relation.getReport() == null)
                mapRelationsByNeed.computeIfAbsent("add", key -> new ArrayList<>()).add(relationWithOwner);
            else {
                // Check case session's updated_at < submission file's updated_at -> file is updated after detect
                // Else all sessions are detected and no file hasn't been updated after detection
                if (relation.getReport().getUpdatedAt().isBefore(relation.getFile().getUpdatedAt()))
                    mapRelationsByNeed.computeIfAbsent("modify", key -> new ArrayList<>()).add(relationWithOwner);
                else
                    mapRelationsByNeed.computeIfAbsent("ignore", key -> new ArrayList<>()).add(relationWithOwner);
            }
        }));
        return mapRelationsByNeed;
    }

    private void handleNeedModifySubmissions(List<RelationWithOwner> needModifyRelations, UserReference reference)
            throws IOException {
        if (needModifyRelations == null || needModifyRelations.isEmpty())
            return;
        // Get list of sessions
        // Get all file ids
        List<SimilarityReport> sessions = new ArrayList<>();
        List<Pair<String, String>> pairIndexWithIds = new ArrayList<>();

        needModifyRelations.forEach(relation -> {
            sessions.add(relation.getRelation().getReport());
            pairIndexWithIds.addAll(relation.getRelation()
                                            .getReport()
                                            .getSources()
                                            .stream()
                                            .map(ReportSourceDocument::getSource)
                                            .map(source -> Pair.of(LanguageUtil.getInstance()
                                                                               .getIndexFromFileName(source.getFileName()),
                                                                   source.getId().toString()))
                                            .toList());
            relation.getRelation().setReport(null);
        });
        // Perform deleting from ES index
        deleteDocumentsFromElasticsearch(pairIndexWithIds);
        // Clear all sessions and their files
        repoSimilarityReport.deleteAll(sessions);

        needModifyRelations.forEach(relation -> detectSubmission(relation, reference));
    }

    private void deleteDocumentsFromElasticsearch(List<Pair<String, String>> pairIndexWithId) throws IOException {
        repoElasticsearchDelete.bulkDeleteByIds(pairIndexWithId);
    }

    private void handleNeedAddSubmissions(List<RelationWithOwner> needAddRelations, UserReference reference) {
        if (needAddRelations == null || needAddRelations.isEmpty())
            return;
        needAddRelations.forEach(relation -> detectSubmission(relation, reference));
    }

    private void detectSubmission(RelationWithOwner relationWithOwner, UserReference reference) {
        // Each relation will have detected request
        SimilarityDetectRequest request = buildRequestFromMoodleFile(relationWithOwner,
                                                                     reference,
                                                                     relationWithOwner.getOwner());
        SimilarityReport session = serviceQuery.createSimilarityReport(request, queryInstruction, user);

        relationWithOwner.getRelation().setReport(session);
    }

    private SimilarityDetectRequest buildRequestFromMoodleFile(RelationWithOwner relation,
                                                               UserReference reference,
                                                               MoodleUser owner) {
        // Enrich zip file from moodle
        byte[] data = getSubmissionFileFromMoodle(relation.getRelation().getFile().getFileUri(), reference);
        List<FileDocument> documents = parseFileDocuments(relation.getRelation().getFile(), data, owner);
        return SimilarityDetectRequest.builder()
                                      .reportName(FileSystemUtil.getFileName(relation.getRelation()
                                                                                     .getFile()
                                                                                     .getFilename()))
                                      .sources(documents)
                                      .author(relation.getOwner().getEmail())
                                      .origin("moodle")
                                      .build();
    }

    private List<FileDocument> parseFileDocuments(SubmissionFile file, byte[] data, MoodleUser owner) {
        String filename = file.getFilename();
        List<FileDocument> documents = new ArrayList<>();
        switch (FilenameUtils.getExtension(filename)) {
            case "" -> {
                return documents;
            }
            case "zip" -> documents.addAll(ZipUtil.getFileDocumentFromZipFile(data, owner.getFullname()));
            default -> {
                try {
                    languageUtil.getIndexFromFileName(filename);
                    // For source code file, only 1 file document is created
                    documents.add(FileDocument.builder()
                                              .content(data)
                                              .fileName(filename)
                                              .author(owner.getFullname())
                                              .build());
                } catch (UnsupportedLanguage ignored) {
                }

            }
        }
        return documents;
    }

    private byte[] getSubmissionFileFromMoodle(String fileUri, UserReference reference) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(fileUri)
                                                           .queryParam("token", reference.getToken());
        ResponseEntity<byte[]> entity = moodleClient.getForEntity(builder.toUriString(), byte[].class);
        if (!entity.getStatusCode().is2xxSuccessful()) {
            log.error("[Service moodle] Fail to get submission file at url: {}", fileUri);
            throw new MoodleSubmissionException("Fail to get submission file at url " + fileUri);
        }
        return entity.getBody();
    }

    @Override
    public void run() {
        // Get list of submissions
        List<Submission> submissions = repoSubmission.findAllByIdIn(submissionIds);
        try {
            // Classify submissions
            // They will be split into 3 groups:
            // + Modify: the submission has been detected, but new file are updated -> need overwrite session
            // + Add: the submission hasn't been detected yet -> detect
            // + Ignore: the submission has been detected, and there is no relation that has been updated -> ignore
            Map<String, List<RelationWithOwner>> mapRelationWithOwnerByNeed = classifySubmissions(submissions);
            handleNeedAddSubmissions(mapRelationWithOwnerByNeed.get("add"), user.getReference());
            handleNeedModifySubmissions(mapRelationWithOwnerByNeed.get("modify"), user.getReference());
            repoSubmission.saveAll(submissions);
        } catch (Exception e) {
            log.error("[Detect selected submissions job] Error: {}. Submissions: {}",
                      e.getMessage(),
                      submissions.stream().map(Submission::getId).toList());
            for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                System.out.printf("Line %d: %s - %s\n",
                                  stackTraceElement.getLineNumber(),
                                  stackTraceElement.getClassName(),
                                  stackTraceElement.getMethodName());
            }
            throw new RuntimeException(e);
        }
        log.info("[Detect selected submissions job] Detect successfully for submissions: {}",
                 submissions.stream().map(Submission::getId).toList());
    }

    @Builder
    @Data
    private static class RelationWithOwner {
        MoodleUser owner;
        RelationSubmissionReport relation;
    }
}
