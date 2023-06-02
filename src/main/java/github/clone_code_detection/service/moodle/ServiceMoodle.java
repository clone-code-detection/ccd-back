package github.clone_code_detection.service.moodle;


import com.fasterxml.jackson.databind.JsonNode;
import github.clone_code_detection.entity.authenication.SignInRequest;
import github.clone_code_detection.entity.authenication.UserImpl;
import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.entity.highlight.document.HighlightSessionDocument;
import github.clone_code_detection.entity.highlight.document.HighlightSingleDocument;
import github.clone_code_detection.entity.highlight.report.HighlightSessionReportDTO;
import github.clone_code_detection.entity.highlight.request.HighlightSessionRequest;
import github.clone_code_detection.entity.index.IndexInstruction;
import github.clone_code_detection.entity.moodle.*;
import github.clone_code_detection.entity.moodle.dto.AssignDTO;
import github.clone_code_detection.entity.moodle.dto.CourseDTO;
import github.clone_code_detection.entity.moodle.dto.CourseOverviewDTO;
import github.clone_code_detection.exceptions.UnsupportedLanguage;
import github.clone_code_detection.exceptions.moodle.MoodleAssignmentException;
import github.clone_code_detection.exceptions.moodle.MoodleAuthenticationException;
import github.clone_code_detection.exceptions.moodle.MoodleCourseException;
import github.clone_code_detection.exceptions.moodle.MoodleSubmissionException;
import github.clone_code_detection.repo.*;
import github.clone_code_detection.service.highlight.ServiceHighlight;
import github.clone_code_detection.util.FileSystemUtil;
import github.clone_code_detection.util.LanguageUtil;
import github.clone_code_detection.util.TimeUtil;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.auth.AuthenticationException;
import org.modelmapper.internal.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@Validated
@Slf4j
@Transactional
public class ServiceMoodle {
    private static final String MOODLE_GET_COURSES_FUNCTION = "core_enrol_get_users_courses";
    private static final String MOODLE_GET_ASSIGNS_FUNCTION = "mod_assign_get_assignments";
    private static final String MOODLE_GET_SUBMISSIONS_FUNCTION = "mod_assign_get_submissions";
    private static final String MOODLE_GET_OWNERS_FUNCTION = "core_user_get_users_by_field";
    private static final String MOODLE_GET_COURSE_DETAIL_FUNCTION = "core_course_get_courses_by_field";
    private static final String MOODLE_GET_SITE_INFO_FUNCTION = "core_webservice_get_site_info";
    private static final LanguageUtil languageUtil = LanguageUtil.getInstance();
    private final RepoHighlightSessionDocument repoHighlightSessionDocument;
    private final RepoMoodleUser repoMoodleUser;
    private final RepoRelationSubmissionSession repoRelationSubmissionSession;
    private final RepoUserReference repoUserReference;
    private final RepoSubmission repoSubmission;
    private final RestTemplate moodleClient;
    private final ServiceHighlight serviceHighlight;
    private final RepoElasticsearchDelete repoElasticsearchDelete;
    @Value("${moodle.web-service}")
    String webServiceName;
    @Value("${moodle.signin-uri}")
    String moodleSigninUri;
    @Value("${moodle.web-service-uri}")
    String moodleWebServiceUri;

    @Autowired
    public ServiceMoodle(RepoUserReference repoUserReference,
                         RepoSubmission repoSubmission,
                         RestTemplate moodleClient,
                         RepoRelationSubmissionSession repoRelationSubmissionSession,
                         RepoMoodleUser repoMoodleUser,
                         ServiceHighlight serviceHighlight,
                         RepoElasticsearchDelete repoElasticsearchDelete,
                         RepoHighlightSessionDocument repoHighlightSessionDocument) {
        this.repoSubmission = repoSubmission;
        this.repoUserReference = repoUserReference;
        this.moodleClient = moodleClient;
        this.repoRelationSubmissionSession = repoRelationSubmissionSession;
        this.repoMoodleUser = repoMoodleUser;
        this.serviceHighlight = serviceHighlight;
        this.repoElasticsearchDelete = repoElasticsearchDelete;
        this.repoHighlightSessionDocument = repoHighlightSessionDocument;
    }

    public static Collection<HighlightSessionRequest> unzipMoodleFileAndGetRequests(@NotNull MultipartFile source)
            throws IOException {
        // Moodle zip file has structure as list of folder, each folder is student's list file submissions
        ArrayList<HighlightSessionRequest> requests = new ArrayList<>(); // Each request will be the highlight session
        // Create zip handler for root zip file which got grom moodle
        try (ZipInputStream zipInputStream = new ZipInputStream(source.getInputStream())) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ZipEntry zipEntry;
            // Loop over each student submission file, which can be source code file or zip file
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                // If entry is the source code file then create new session
                String author = zipEntry.getName().split("/")[0];
                String sessionName = String.format("%s/%s",
                                                   FileSystemUtil.getFileName(source.getOriginalFilename()),
                                                   FileSystemUtil.getFileName(zipEntry.getName()));
                if (zipEntry.isDirectory()) {
                    continue;
                }
                byteArrayOutputStream.reset();
                // get the exists highlight session request or new request which has been added into Collection
                HighlightSessionRequest request = getExistRequestOrCreateNew(requests, sessionName);

                Collection<FileDocument> fileDocuments = new ArrayList<>();
                zipInputStream.transferTo(byteArrayOutputStream);
                switch (FilenameUtils.getExtension(zipEntry.getName())) {
                    case "" -> {continue;}
                    case "zip" ->
                            fileDocuments.addAll(getFileDocumentFromNestedZipFile(byteArrayOutputStream.toByteArray(),
                                                                                  author));
                    default -> {
                        try {
                            languageUtil.getIndexFromFileName(zipEntry.getName());
                            // For source code file, only 1 file document is created
                            fileDocuments.add(FileDocument.builder()
                                                          .content(byteArrayOutputStream.toByteArray())
                                                          .fileName(zipEntry.getName())
                                                          .author(author)
                                                          .build());
                        } catch (UnsupportedLanguage ignored) {}

                    }
                }
                if (!fileDocuments.isEmpty()) request.setSources(fileDocuments);
            }
        }

        return requests;
    }

    private static UriComponentsBuilder buildDefaultUriBuilder(String token, String function, String path) {
        return UriComponentsBuilder.fromPath(path)
                                   .queryParam("wstoken", token)
                                   .queryParam("wsfunction", function)
                                   .queryParam("moodlewsrestformat", "json");
    }

    private static Collection<FileDocument> getFileDocumentFromNestedZipFile(byte @NotNull [] bytes,
                                                                             @NotNull String author) {
        Collection<FileDocument> fileDocuments = new ArrayList<>();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        try (ZipInputStream zipInputStream = new ZipInputStream(byteArrayInputStream)) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (zipEntry.isDirectory()) continue;
                byteArrayOutputStream.reset();
                extracted(author, fileDocuments, byteArrayOutputStream, zipInputStream, zipEntry);
            }
            zipInputStream.closeEntry();
        } catch (IOException e) {
            log.error("[Service moodle] Got error while unzip project file: {}", e.getMessage());
            return new ArrayList<>();
        }
        return fileDocuments;
    }

    private static void extracted(String author,
                                  Collection<FileDocument> fileDocuments,
                                  ByteArrayOutputStream byteArrayOutputStream,
                                  ZipInputStream zipInputStream,
                                  ZipEntry zipEntry) throws IOException {
        try {
            languageUtil.getIndexFromFileName(zipEntry.getName());
            zipInputStream.transferTo(byteArrayOutputStream);
            fileDocuments.add(FileDocument.builder()
                                          .author(author)
                                          .fileName(zipEntry.getName())
                                          .content(byteArrayOutputStream.toByteArray())
                                          .build());
        } catch (UnsupportedLanguage ignored) {}
    }

    private static HighlightSessionRequest getExistRequestOrCreateNew(ArrayList<HighlightSessionRequest> requests,
                                                                      String sessionName) {
        for (HighlightSessionRequest request : requests) {
            if (request.getSessionName().equals(sessionName)) return request;
        }
        HighlightSessionRequest request = HighlightSessionRequest.builder().sessionName(sessionName).build();
        requests.add(request);
        return request;
    }

    @NotNull
    private static <T> PageImpl<T> getListWithPageable(Pageable pageable, List<T> list) {
        if (pageable.getPageNumber() * pageable.getPageSize() > list.size())
            return new PageImpl<>(new ArrayList<>(), pageable, list.size());
        return new PageImpl<>(list.subList((int) pageable.getOffset(),
                                           (int) Math.min(pageable.getPageSize() + pageable.getOffset(), list.size())),
                              pageable,
                              list.size());
    }

    public void linkCurrentUserToMoodleAccount(SignInRequest request) throws AuthenticationException {
        // Get token and userid from moodle
        UserImpl user = ServiceHighlight.getUserFromContext();
        if (user == null) throw new AuthenticationException("User not found");
        if (user.getReference() == null) {
            UserReference reference = getMoodleAccount(request);
            reference.setInternalUser(user);
            user.setReference(reference);
        }
    }

    public CourseOverviewDTO getCourseOverview(Pageable pageable) {
        UserImpl user = ServiceHighlight.getUserFromContext();
        if (user == null || user.getId() == null) {
            log.error("[Service moodle] Fail to get user information");
            throw new MoodleCourseException("Fail to get user information");
        }
        ResponseEntity<JsonNode> entity = getCoursesOfUser(user.getReference());
        List<Course> courses = Course.asList(Objects.requireNonNull(entity.getBody()));
        courses.sort(Comparator.comparing(Course::getId).reversed());
        return CourseOverviewDTO.builder()
                                .courses(getListWithPageable(pageable, courses))
                                .user(user.getReference().getReferenceUser().toDTO())
                                .build();
    }

    public CourseDTO getCourseDetail(long courseId, Pageable pageable) {
        UserImpl user = ServiceHighlight.getUserFromContext();
        if (user == null || user.getId() == null) {
            log.error("[Service moodle] Fail to get user information");
            throw new MoodleCourseException("Fail to get user information");
        }
        CourseDTO overviewDTO = CourseDTO.builder().build();
        List<Assign> assigns = enrichAssignments(user.getReference(), courseId);
        assigns.sort(Comparator.comparing(Assign::getId));
        overviewDTO.setCourse(enrichCourseDetail(courseId, user.getReference()));
        overviewDTO.setAssigns(getListWithPageable(pageable, assigns));
        return overviewDTO;
    }

    public AssignDTO getAssignDetail(long courseId, long assignId, Pageable pageable) {
        UserImpl user = ServiceHighlight.getUserFromContext();
        if (user == null || user.getId() == null) {
            log.error("[Service moodle] Fail to get user information");
            throw new MoodleCourseException("Fail to get user information");
        }
        UserReference reference = repoUserReference.findFirstByInternalUserId(user.getId());
        List<Submission> submissions = getSubmissionsInCourse(courseId, new ArrayList<>(List.of(assignId)), reference);
        submissions = repoSubmission.saveAll(submissions);
        submissions.sort(Comparator.comparing(Submission::getId));
        Assign assign = enrichAssignments(reference, courseId).stream()
                                                              .filter(e -> e.getId() == assignId)
                                                              .findFirst()
                                                              .orElse(null);
        if (pageable.getPageNumber() * pageable.getPageSize() > submissions.size()) return AssignDTO.builder()
                                                                                                    .submissions(new PageImpl<>(
                                                                                                            new ArrayList<>(),
                                                                                                            pageable,
                                                                                                            submissions.size()))
                                                                                                    .assign(assign)
                                                                                                    .build();
        return AssignDTO.builder()
                        .submissions(new PageImpl<>(submissions.subList((int) pageable.getOffset(),
                                                                        (int) Math.min(pageable.getPageSize() + pageable.getOffset(),
                                                                                       submissions.size()))
                                                               .stream()
                                                               .map(Submission::toDTO)
                                                               .toList(), pageable, submissions.size()))
                        .assign(assign)
                        .build();
    }

    public MoodleResponse detectSubmissions(List<Long> submissionIds) throws IOException {
        UserImpl user = ServiceHighlight.getUserFromContext();
        if (user == null || user.getId() == null) {
            log.error("[Service moodle] Fail to get user information");
            throw new MoodleCourseException("Fail to get user information");
        }
        List<HighlightSessionReportDTO> sessionReportDTOS = new ArrayList<>();
        // Get list of submissions
        List<Submission> submissions = repoSubmission.findAllByIdIn(submissionIds);
        // Classify submissions
        // They will be split into 3 groups:
        // + Modify: the submission has been detected, but new file are updated -> need overwrite session
        // + Add: the submission hasn't been detected yet -> detect
        // + Ignore: the submission has been detected, and there is no relation that has been updated -> ignore
        Map<String, List<RelationWithOwner>> mapRelationWithOwnerByNeed = classifySubmissions(submissions);
        sessionReportDTOS.addAll(handleNeedAddSubmissions(mapRelationWithOwnerByNeed.get("add"), user.getReference()));
        sessionReportDTOS.addAll(handleNeedModifySubmissions(mapRelationWithOwnerByNeed.get("modify"),
                                                             user.getReference()));
        sessionReportDTOS.addAll(handleNeedIgnoreSubmissions(mapRelationWithOwnerByNeed.get("ignore")));
        repoSubmission.saveAll(submissions);
        return MoodleResponse.builder().data(sessionReportDTOS).build();
    }

    private List<Assign> enrichAssignments(UserReference reference, long courseId) {
        UriComponentsBuilder builder = buildDefaultUriBuilder(reference.getToken(),
                                                              MOODLE_GET_ASSIGNS_FUNCTION,
                                                              moodleWebServiceUri);
        builder.queryParam("courseids[]", courseId);
        ResponseEntity<JsonNode> entity = moodleClient.getForEntity(moodleClient.getUriTemplateHandler()
                                                                                .expand(builder.toUriString())
                                                                                .toString(), JsonNode.class);
        if (!entity.getStatusCode().is2xxSuccessful()) {
            log.error("[Service moodle] Fail to get assigns by course id");
            throw new MoodleAssignmentException("Fail to enrich assignments from course id");
        }

        return fulfillAssignments(Objects.requireNonNull(entity.getBody()));
    }

    private List<Assign> fulfillAssignments(JsonNode data) {
        JsonNode moodleCourses = data.get("courses");
        List<Assign> assigns = new ArrayList<>();
        moodleCourses.forEach(moodleCourse -> assigns.addAll(Assign.from(moodleCourse.get("assignments"))));
        return assigns;
    }

    private List<Submission> getSubmissionsInCourse(long courseId, List<Long> assignIds, UserReference reference) {
        UriComponentsBuilder builder = buildDefaultUriBuilder(reference.getToken(),
                                                              MOODLE_GET_SUBMISSIONS_FUNCTION,
                                                              moodleWebServiceUri);
        assignIds.forEach(assignId -> builder.queryParam("assignmentids[]", assignId));
        ResponseEntity<JsonNode> entity = moodleClient.getForEntity(moodleClient.getUriTemplateHandler()
                                                                                .expand(builder.toUriString())
                                                                                .toString(), JsonNode.class);
        if (!entity.getStatusCode().is2xxSuccessful()) {
            log.error("[Service moodle] Fail to get submissions by assignments");
            throw new MoodleSubmissionException("Fail to enrich submissions");
        }
        return parseSubmissions(Objects.requireNonNull(entity.getBody()).get("assignments"), courseId, reference);
    }

    private List<Submission> parseSubmissions(@NotNull JsonNode assignments, long courseId, UserReference reference) {
        List<Submission> submissions = new ArrayList<>();
        Set<Long> ownerIds = new HashSet<>();
        Map<Long, List<Long>> mapListSubmissionIdByOwnerId = new HashMap<>();
        assignments.forEach(assignment -> {
            long assignId = assignment.get("assignmentid").asLong();
            assignment.get("submissions").forEach(inputSubmission -> {
                long ownerId = inputSubmission.get("userid").asLong();
                long submissionId = inputSubmission.get("id").asLong();
                if (ownerId == reference.getReferenceUser().getReferenceUserId()) return;

                MoodleUser owner = repoMoodleUser.findFirstByReferenceUserId(ownerId);
                if (owner == null) {
                    ownerIds.add(ownerId);
                    mapListSubmissionIdByOwnerId.computeIfAbsent(ownerId, key -> new ArrayList<>()).add(submissionId);
                }
                Submission submission = repoSubmission.findFirstByReferenceSubmissionId(inputSubmission.get("id")
                                                                                                       .asLong());
                List<RelationSubmissionSession> relations = getSubmissionRelations(inputSubmission.get("plugins"));
                if (submission == null) {
                    // Case new submission
                    submission = Submission.builder()
                                           .referenceSubmissionId(submissionId)
                                           .createdAt(TimeUtil.parseZoneDateTime(inputSubmission.get("timecreated")
                                                                                                .asLong()))
                                           .updatedAt(TimeUtil.parseZoneDateTime(inputSubmission.get("timemodified")
                                                                                                .asLong()))
                                           .courseId(courseId)
                                           .assignId(assignId)
                                           .relations(relations)
                                           .owner(owner)
                                           .build();
                } else {
                    // Update submission if any new change
                    submission.setUpdatedAt(TimeUtil.parseZoneDateTime(inputSubmission.get("timemodified").asLong()));
                    updateSubmissionRelations(submission, relations);
                }
                submissions.add(submission);
            });
        });
        if (ownerIds.isEmpty()) return submissions;
        // Enrich owner information
        Map<Long, MoodleUser> mapOwnerBySubmissionReferenceId = getCourseSubmissionOwners(ownerIds,
                                                                                          mapListSubmissionIdByOwnerId,
                                                                                          reference);
        submissions.forEach(submission -> {
            if (submission.getOwner() == null)
                submission.setOwner(mapOwnerBySubmissionReferenceId.get(submission.getReferenceSubmissionId()));
        });
        return submissions;
    }

    private void updateSubmissionRelations(Submission submission, List<RelationSubmissionSession> newRelations) {
        List<RelationSubmissionSession> currentRelations = submission.getRelations();
        // Modified current relation that still exists in new relation
        List<RelationSubmissionSession> needDeleteRelations = new ArrayList<>();
        List<RelationSubmissionSession> finalRelations = new ArrayList<>();
        Map<String, RelationSubmissionSession> mapNewRelationByIndex = new HashMap<>();
        // Insert for new that is not found in current
        newRelations.forEach(newRelation -> {
            if (currentRelations.stream()
                                .map(RelationSubmissionSession::getFile)
                                .map(SubmissionFile::getFilename)
                                .noneMatch(currentFilename -> currentFilename.equals(newRelation.getFile()
                                                                                                .getFilename()))) {
                // Insert
                finalRelations.add(newRelation);
            }
            mapNewRelationByIndex.put(newRelation.getFile().getFilename(), newRelation);
        });
        currentRelations.forEach(currentRelation -> {
            RelationSubmissionSession newRelation = mapNewRelationByIndex.get(currentRelation.getFile().getFilename());
            if (newRelation == null) {
                // Case current not found in new then delete
                needDeleteRelations.add(currentRelation);
            } else {
                // If current found in new then modify the submission file -> update
                currentRelation.getFile().setUpdatedAt(newRelation.getFile().getUpdatedAt());
                currentRelation.getFile().setFileUri(newRelation.getFile().getFileUri());
                finalRelations.add(currentRelation);
            }
        });
        repoRelationSubmissionSession.deleteAll(needDeleteRelations);
        submission.setRelations(finalRelations);
    }

    private Map<Long, MoodleUser> getCourseSubmissionOwners(Set<Long> ownerIds,
                                                            Map<Long, List<Long>> mapListSubmissionIdByOwnerId,
                                                            UserReference reference) {
        Map<Long, MoodleUser> mapOwnerBySubmissionReferenceId = new HashMap<>();
        List<MoodleUser> owners = getOwnersFromIds(ownerIds, reference);
        owners.forEach(owner -> mapListSubmissionIdByOwnerId.get(owner.getReferenceUserId())
                                                            .forEach(submissionId -> mapOwnerBySubmissionReferenceId.put(
                                                                    submissionId,
                                                                    owner)));
        return mapOwnerBySubmissionReferenceId;
    }

    private List<MoodleUser> getOwnersFromIds(Set<Long> ownerIds, UserReference reference) {
        UriComponentsBuilder builder = buildDefaultUriBuilder(reference.getToken(),
                                                              MOODLE_GET_OWNERS_FUNCTION,
                                                              moodleWebServiceUri).queryParam("field", "id");
        ownerIds.forEach(id -> builder.queryParam("values[]", id));
        ResponseEntity<JsonNode> entity = moodleClient.getForEntity(moodleClient.getUriTemplateHandler()
                                                                                .expand(builder.toUriString()),
                                                                    JsonNode.class);
        if (!entity.getStatusCode().is2xxSuccessful()) {
            log.error("[Service moodle] Fail to enrich submission owners");
            throw new MoodleSubmissionException("Fail to enrich submission owners");
        }
        return parseSubmissionOwners(Objects.requireNonNull(entity.getBody()));
    }

    private List<MoodleUser> parseSubmissionOwners(JsonNode data) {
        List<MoodleUser> owners = new ArrayList<>();
        data.forEach(owner -> owners.add(MoodleUser.from(owner)));

        return owners;
    }

    private List<RelationSubmissionSession> getSubmissionRelations(JsonNode plugins) {
        List<RelationSubmissionSession> relationSubmissionSessions = new ArrayList<>();
        plugins.forEach(plugin -> {
            if (plugin.get("type").asText().equals("file")) {
                plugin.get("fileareas").forEach(fileareas -> {
                    if (fileareas.get("area").asText().equals("submission_files")) {
                        fileareas.get("files").forEach(file -> {
                            SubmissionFile submissionFile = SubmissionFile.builder()
                                                                          .filename(file.get("filename").asText())
                                                                          .fileUri(file.get("fileurl").asText())
                                                                          .mimetype(file.get("mimetype").asText())
                                                                          .updatedAt(TimeUtil.parseZoneDateTime(file.get(
                                                                                  "timemodified").asLong()))
                                                                          .build();
                            relationSubmissionSessions.add(RelationSubmissionSession.builder()
                                                                                    .file(submissionFile)
                                                                                    .build());
                        });
                    }
                });
            }
        });
        return relationSubmissionSessions;
    }

    @NotNull
    private ResponseEntity<JsonNode> getCoursesOfUser(UserReference reference) {
        UriComponentsBuilder builder = buildDefaultUriBuilder(reference.getToken(),
                                                              MOODLE_GET_COURSES_FUNCTION,
                                                              moodleWebServiceUri).queryParam("userid",
                                                                                              reference.getReferenceUser()
                                                                                                       .getReferenceUserId());
        ResponseEntity<JsonNode> entity = moodleClient.getForEntity(moodleClient.getUriTemplateHandler()
                                                                                .expand(builder.toUriString())
                                                                                .toString(), JsonNode.class);
        if (!entity.getStatusCode().is2xxSuccessful()) {
            log.error("[Service moodle] Fail to get courses from Moodle");
            throw new MoodleCourseException("Fail to get courses from Moodle");
        }
        return entity;
    }

    @NotNull
    private UserReference getMoodleAccount(SignInRequest request) {
        // Call moodle to get user_id and token
        UriComponentsBuilder getTokenParams = UriComponentsBuilder.fromPath(moodleSigninUri)
                                                                  .queryParam("username", request.getUsername())
                                                                  .queryParam("password", request.getPassword())
                                                                  .queryParam("service", webServiceName);
        ResponseEntity<JsonNode> entity = moodleClient.getForEntity(moodleClient.getUriTemplateHandler()
                                                                                .expand(getTokenParams.toUriString())
                                                                                .toString(), JsonNode.class);
        if (!entity.getStatusCode().is2xxSuccessful() || entity.getBody() == null) {
            log.error("[Service moodle] Can sign in by moodle account");
            throw new MoodleAuthenticationException("Fail to signin by moodle account " + request.getUsername());
        }
        String token = entity.getBody().get("token").asText();
        UserReference reference = repoUserReference.findFirstByToken(token);
        if (reference == null) {
            // Enrich user id by token
            UriComponentsBuilder getSiteInfoParams = buildDefaultUriBuilder(token,
                                                                            MOODLE_GET_SITE_INFO_FUNCTION,
                                                                            moodleWebServiceUri);
            entity = moodleClient.getForEntity(moodleClient.getUriTemplateHandler()
                                                           .expand(getSiteInfoParams.toUriString())
                                                           .toString(), JsonNode.class);
            if (!entity.getStatusCode().is2xxSuccessful() || entity.getBody() == null) {
                log.error("[Service moodle] Can enrich user info by moodle account");
                throw new MoodleAuthenticationException("Fail to enrich user info by moodle account " + request.getUsername());
            }
            long referenceUserId = entity.getBody().get("userid").asLong();
            reference = UserReference.builder().token(token).build();
            // Enrich user info from user id
            UriComponentsBuilder builder = buildDefaultUriBuilder(reference.getToken(),
                                                                  MOODLE_GET_OWNERS_FUNCTION,
                                                                  moodleWebServiceUri).queryParam("field", "id")
                                                                                      .queryParam("values[]",
                                                                                                  referenceUserId);
            entity = moodleClient.getForEntity(moodleClient.getUriTemplateHandler()
                                                           .expand(builder.toUriString())
                                                           .toString(), JsonNode.class);
            if (!entity.getStatusCode().is2xxSuccessful()) {
                log.error("[Service moodle] Fail to enrich submission owners");
                throw new MoodleSubmissionException("Fail to enrich submission owners");
            }
            reference.setReferenceUser(MoodleUser.from(Objects.requireNonNull(entity.getBody()).get(0)));
        }
        return reference;
    }

    private Course enrichCourseDetail(long courseId, UserReference reference) {
        UriComponentsBuilder builder = buildDefaultUriBuilder(reference.getToken(),
                                                              MOODLE_GET_COURSE_DETAIL_FUNCTION,
                                                              moodleWebServiceUri);
        builder.queryParam("field", "id");
        builder.queryParam("value", courseId);
        ResponseEntity<JsonNode> entity = moodleClient.getForEntity(moodleClient.getUriTemplateHandler()
                                                                                .expand(builder.toUriString()),
                                                                    JsonNode.class);
        if (!entity.getStatusCode().is2xxSuccessful()) {
            log.error("[Service moodle] Fail to enrich course detail content");
            throw new MoodleCourseException("Fail to enrich course detail");
        }
        return Course.asDetail(Objects.requireNonNull(entity.getBody()).get("courses").get(0));
    }

    private Collection<HighlightSessionReportDTO> handleNeedIgnoreSubmissions(List<RelationWithOwner> relationWithOwners) {
        if (relationWithOwners == null || relationWithOwners.isEmpty()) return new ArrayList<>();
        return relationWithOwners.stream()
                                 .map(RelationWithOwner::getRelation)
                                 .map(RelationSubmissionSession::getSession)
                                 .map(HighlightSessionReportDTO::from)
                                 .toList();
    }

    private Collection<HighlightSessionReportDTO> handleNeedModifySubmissions(List<RelationWithOwner> needModifyRelations,
                                                                              UserReference reference)
            throws IOException {
        if (needModifyRelations == null || needModifyRelations.isEmpty()) return new ArrayList<>();
        // Get list of sessions
        // Get all file ids
        List<HighlightSessionDocument> sessions = new ArrayList<>();
        List<Pair<String, String>> pairIndexWithIds = new ArrayList<>();

        needModifyRelations.forEach(relation -> {
            sessions.add(relation.getRelation().getSession());
            pairIndexWithIds.addAll(relation.getRelation()
                                            .getSession()
                                            .getMatches()
                                            .stream()
                                            .map(HighlightSingleDocument::getSource)
                                            .map(source -> Pair.of(LanguageUtil.getInstance()
                                                                               .getIndexFromFileName(source.getFileName()),
                                                                   source.getId().toString()))
                                            .toList());
            relation.getRelation().setSession(null);
        });
        // Perform deleting from ES index
        deleteDocumentsFromElasticsearch(pairIndexWithIds);
        // Clear all sessions and their files
        repoHighlightSessionDocument.deleteAll(sessions);

        return needModifyRelations.stream().map(relation -> detectSubmission(relation, reference)).toList();
    }

    private void deleteDocumentsFromElasticsearch(List<Pair<String, String>> pairIndexWithId) throws IOException {
        repoElasticsearchDelete.bulkDeleteByIds(pairIndexWithId);
    }

    private Collection<HighlightSessionReportDTO> handleNeedAddSubmissions(List<RelationWithOwner> needAddRelations,
                                                                           UserReference reference) {
        if (needAddRelations == null || needAddRelations.isEmpty()) return new ArrayList<>();
        List<HighlightSessionReportDTO> reportDTOS = new ArrayList<>();
        needAddRelations.forEach(relation -> reportDTOS.add(detectSubmission(relation, reference)));
        return reportDTOS;
    }

    private HighlightSessionReportDTO detectSubmission(RelationWithOwner relationWithOwner, UserReference reference) {
        // Each relation will have detected request
        HighlightSessionRequest request = buildRequestFromMoodleFile(relationWithOwner.getRelation().getFile(),
                                                                     reference,
                                                                     relationWithOwner.getOwner());
        HighlightSessionDocument session = serviceHighlight.createHighlightSession(request,
                                                                                   IndexInstruction.getDefaultInstruction());
        relationWithOwner.getRelation().setSession(session);
        return HighlightSessionReportDTO.from(session);
    }

    private HighlightSessionRequest buildRequestFromMoodleFile(SubmissionFile file,
                                                               UserReference reference,
                                                               MoodleUser owner) {
        // Enrich zip file from moodle
        byte[] data = getSubmissionFileFromMoodle(file.getFileUri(), reference);
        List<FileDocument> documents = parseFileDocuments(file, data, owner);
        return HighlightSessionRequest.builder()
                                      .sessionName(FileSystemUtil.getFileName(file.getFilename()))
                                      .sources(documents)
                                      .build();
    }

    private List<FileDocument> parseFileDocuments(SubmissionFile file, byte[] data, MoodleUser owner) {
        String filename = file.getFilename();
        List<FileDocument> documents = new ArrayList<>();
        switch (FilenameUtils.getExtension(filename)) {
            case "" -> {return documents;}
            case "zip" -> documents.addAll(getFileDocumentFromNestedZipFile(data, owner.getFullname()));
            default -> {
                try {
                    languageUtil.getIndexFromFileName(filename);
                    // For source code file, only 1 file document is created
                    documents.add(FileDocument.builder()
                                              .content(data)
                                              .fileName(filename)
                                              .author(owner.getFullname())
                                              .build());
                } catch (UnsupportedLanguage ignored) {}

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

    private Map<String, List<RelationWithOwner>> classifySubmissions(List<Submission> submissions) {
        Map<String, List<RelationWithOwner>> mapRelationsByNeed = new HashMap<>();
        submissions.forEach(submission -> submission.getRelations().forEach(relation -> {
            RelationWithOwner relationWithOwner = RelationWithOwner.builder()
                                                                   .owner(submission.getOwner())
                                                                   .relation(relation)
                                                                   .build();
            // If session is null then this relation need to add
            // If submission relation has session id = null then add new
            if (relation.getSession() == null)
                mapRelationsByNeed.computeIfAbsent("add", key -> new ArrayList<>()).add(relationWithOwner);
            else {
                // Check case session's updated_at < submission file's updated_at -> file is updated after detect
                // Else all sessions are detected and no file hasn't been updated after detection
                if (relation.getSession().getUpdatedAt().isBefore(relation.getFile().getUpdatedAt()))
                    mapRelationsByNeed.computeIfAbsent("modify", key -> new ArrayList<>()).add(relationWithOwner);
                else mapRelationsByNeed.computeIfAbsent("ignore", key -> new ArrayList<>()).add(relationWithOwner);
            }
        }));
        return mapRelationsByNeed;
    }


    @Builder
    @Data
    private static class RelationWithOwner {
        MoodleUser owner;
        RelationSubmissionSession relation;
    }
}
