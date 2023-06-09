package github.clone_code_detection.service.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import github.clone_code_detection.entity.authenication.UserImpl;
import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.entity.highlight.document.ReportSourceDocument;
import github.clone_code_detection.entity.highlight.document.ReportTargetDocument;
import github.clone_code_detection.entity.highlight.document.SimilarityReport;
import github.clone_code_detection.entity.highlight.dto.SimilarityReportDetailDTO;
import github.clone_code_detection.entity.highlight.request.SimilarityDetectRequest;
import github.clone_code_detection.entity.index.IndexInstruction;
import github.clone_code_detection.entity.query.QueryInstruction;
import github.clone_code_detection.exceptions.highlight.ElasticsearchQueryException;
import github.clone_code_detection.repo.RepoElasticsearchQuery;
import github.clone_code_detection.repo.RepoFileDocument;
import github.clone_code_detection.repo.RepoReportSourceDocument;
import github.clone_code_detection.repo.RepoSimilarityReport;
import github.clone_code_detection.service.index.ServiceIndex;
import github.clone_code_detection.service.user.ServiceAuthentication;
import github.clone_code_detection.util.FileSystemUtil;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;

@Service
@Slf4j
public class ServiceQuery {
    private final RepoElasticsearchQuery repoElasticsearchQuery;
    private final RepoReportSourceDocument repoReportSourceDocument;
    private final RepoSimilarityReport repoSimilarityReport;
    private final RepoFileDocument repoFileDocument;
    private final ServiceIndex serviceIndex;
    private final ThreadPoolExecutor threadPoolExecutor;
    private final DetectSimilarityJobFactory detectSimilarityJobFactory;

    @Autowired
    public ServiceQuery(RepoElasticsearchQuery repoElasticsearchQuery,
                        RepoReportSourceDocument repoHighlightSessionDocument,
                        RepoSimilarityReport repoSimilarityReport,
                        RepoFileDocument repoFileDocument,

                        ServiceIndex serviceIndex,
                        ThreadPoolExecutor threadPoolExecutor,
                        @Lazy DetectSimilarityJobFactory detectSimilarityJobFactory) {
        this.repoElasticsearchQuery = repoElasticsearchQuery;
        this.repoReportSourceDocument = repoHighlightSessionDocument;
        this.repoSimilarityReport = repoSimilarityReport;
        this.repoFileDocument = repoFileDocument;
        this.serviceIndex = serviceIndex;
        this.threadPoolExecutor = threadPoolExecutor;
        this.detectSimilarityJobFactory = detectSimilarityJobFactory;
    }

    private static TargetMatchOverview extract(ReportTargetDocument document) {
        UserImpl user = document.getTarget()
                                .getUser();
        String userName = user != null ? user.getUsername() : "anonymous";
        String targetId = document.getId()
                                  .toString();
        Float score = document.getScore();
        return TargetMatchOverview.builder()
                                  .author(userName)
                                  .id(targetId)
                                  .score(score)
                                  .build();
    }

    public Collection<TargetMatchOverview> handle(String id) {
        UUID fromString;
        try {
            fromString = UUID.fromString(id);
        } catch (Exception ig) {
            throw new RuntimeException("Invalid id", ig);
        }
        ReportSourceDocument singleDocument = repoReportSourceDocument.findById(fromString)
                                                                      .orElseThrow();
        return singleDocument.getMatches()
                             .stream()
                             .map(ServiceQuery::extract)
                             .toList();
    }

    /**
     * For each file, query with highlight enabled
     */
    public ReportSourceDocument extractSingleDocument(FileDocument source, QueryInstruction queryInstruction) {
        SearchResponse searchResponse;
        try {
            searchResponse = repoElasticsearchQuery.query(queryInstruction);
        } catch (IOException e) {
            log.error("Error querying elasticsearch", e);
            throw new ElasticsearchQueryException("[Service highlight] Failed to query es");
        }
        // parse response
        return parseResponse(source, searchResponse);
    }

    /**
     * Extract match fields from es search response
     */
    public ReportSourceDocument parseResponse(FileDocument source, SearchResponse search) {
        ReportSourceDocument.ReportSourceDocumentBuilder builder = ReportSourceDocument.builder();
        // get hits
        Collection<ReportTargetDocument> matches = new ArrayList<>();
        ReportSourceDocument reportSourceDocument = builder.source(source)
                                                           .matches(matches)
                                                           .build();
        for (SearchHit hit : search.getHits()) {
            String id = hit.getId();
            UUID fromString;
            try {
                fromString = UUID.fromString(id);
            } catch (Exception ig) {
                continue;
            }
            Optional<FileDocument> fileDocument = repoFileDocument.findById(fromString);
            if (fileDocument.isEmpty()) continue;
            ReportTargetDocument singleMatch = ReportTargetDocument.builder()
                                                                   .score(hit.getScore())
                                                                   .source(reportSourceDocument)
                                                                   .target(fileDocument.get())
                                                                   .build();
            matches.add(singleMatch);
        }
        return reportSourceDocument;
    }

    @Builder
    public static class TargetMatchOverview {
        @JsonProperty("author")
        private String author;

        @JsonProperty("score")
        private double score;

        @JsonProperty("target_match_id")
        private String id;
    }

    // query
    @Transactional
    public SimilarityReport createSimilarityReport(SimilarityDetectRequest request, QueryInstruction queryInstruction) {
        SimilarityReport similarityReport = createEmptyReport(request);
        // Assign session id of empty highlight session into each source document
        Collection<FileDocument> sourceDocuments = request.getSources();
        // Save file linking id of session
        sourceDocuments = repoFileDocument.saveAll(sourceDocuments);
        IndexInstruction indexInstruction = IndexInstruction.builder()
                                                            .files(sourceDocuments)
                                                            .build();
        DetectSimilarityJob.DetectSimilarityJobBuilder builder = DetectSimilarityJob.builder()
                                                                                    .reportId(similarityReport.getId())
                                                                                    .instruction(indexInstruction)
                                                                                    .queryInstruction(queryInstruction);
        DetectSimilarityJob detectSimilarityJob = detectSimilarityJobFactory.newInstance(builder);

        threadPoolExecutor.submit(detectSimilarityJob);
        return similarityReport;
    }

    @NotNull
    private SimilarityReport createEmptyReport(SimilarityDetectRequest request) {
        // Create new empty highlight session
        SimilarityReport.SimilarityReportBuilder sessionBuilder = SimilarityReport.builder();
        SimilarityReport similarityReport = sessionBuilder.build();
        similarityReport.setUser(ServiceAuthentication.getUserFromContext());
        similarityReport.setName(request.getReportName());
        return repoSimilarityReport.save(similarityReport);
    }

    @Transactional
    public SimilarityReport createSimilarityReport(SimilarityDetectRequest request,
                                                   QueryInstruction queryInstruction,
                                                   UserImpl user) {
        SimilarityReport similarityReport = createEmptyReport(request, user);
        // Assign session id of empty highlight session into each source document
        Collection<FileDocument> sourceDocuments = request.getSources();
        // Save file linking id of session
        sourceDocuments = repoFileDocument.saveAll(sourceDocuments);
        IndexInstruction indexInstruction = IndexInstruction.builder()
                                                            .files(sourceDocuments)
                                                            .build();

        DetectSimilarityJob.DetectSimilarityJobBuilder builder = DetectSimilarityJob.builder()
                                                                                    .reportId(similarityReport.getId())
                                                                                    .instruction(indexInstruction)
                                                                                    .queryInstruction(queryInstruction);

        DetectSimilarityJob detectSimilarityJob = detectSimilarityJobFactory.newInstance(builder);
        threadPoolExecutor.submit(detectSimilarityJob);
        return similarityReport;
    }

    @NotNull
    private SimilarityReport createEmptyReport(SimilarityDetectRequest request, UserImpl user) {
        // Create new empty highlight session
        SimilarityReport.SimilarityReportBuilder sessionBuilder = SimilarityReport.builder();
        SimilarityReport similarityReport = sessionBuilder.build();
        similarityReport.setUser(user);
        similarityReport.setName(request.getReportName());
        return repoSimilarityReport.save(similarityReport);
    }

    @Transactional
    public SimilarityReportDetailDTO detectSync(@NotNull MultipartFile source, QueryInstruction queryInstruction) {
        // Validate and extract file from source
        FileSystemUtil.validate(source);
        Collection<FileDocument> sourceDocuments = FileSystemUtil.extractDocuments(source);
        // Highlight source documents
        SimilarityReport.SimilarityReportBuilder sessionBuilder = SimilarityReport.builder();
        List<ReportSourceDocument> hits = new ArrayList<>();
        for (FileDocument sourceDocument : sourceDocuments) {
            // for each document, get highlight request
            ReportSourceDocument reportSourceDocument = extractSingleDocument(sourceDocument,
                    queryInstruction);
            hits.add(reportSourceDocument);
        }
        // Build highlight session
        sessionBuilder.sources(hits);
        SimilarityReport similarityReport = sessionBuilder.build();
        similarityReport.setUser(ServiceAuthentication.getUserFromContext());
        similarityReport.setName(FileSystemUtil.getFileName(source));
        similarityReport = repoSimilarityReport.save(similarityReport);

        // Save source files before indexing
        sourceDocuments = repoFileDocument.saveAll(sourceDocuments);
        // Index the file into es
        IndexInstruction indexInstruction = IndexInstruction.builder()
                                                            .files(sourceDocuments)
                                                            .build();
        serviceIndex.indexAllDocuments(indexInstruction);
        return SimilarityReportDetailDTO.from(similarityReport);
    }
}
