package github.clone_code_detection.service.moodle;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import github.clone_code_detection.entity.authenication.SignInRequest;
import github.clone_code_detection.entity.authenication.UserImpl;
import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.entity.highlight.document.HighlightSessionDocument;
import github.clone_code_detection.entity.highlight.document.HighlightSingleDocument;
import github.clone_code_detection.entity.highlight.report.HighlightSessionReportDTO;
import github.clone_code_detection.entity.highlight.request.HighlightSessionRequest;
import github.clone_code_detection.entity.index.IndexInstruction;
import github.clone_code_detection.entity.moodle.*;
import github.clone_code_detection.exceptions.UnsupportedLanguage;
import github.clone_code_detection.exceptions.moodle.MoodleAssignmentException;
import github.clone_code_detection.exceptions.moodle.MoodleCourseException;
import github.clone_code_detection.exceptions.moodle.MoodleSubmissionException;
import github.clone_code_detection.repo.*;
import github.clone_code_detection.service.highlight.ServiceHighlight;
import github.clone_code_detection.util.FileSystemUtil;
import github.clone_code_detection.util.LanguageUtil;
import github.clone_code_detection.util.TimeUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.modelmapper.internal.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
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
    private static final LanguageUtil languageUtil = LanguageUtil.getInstance();
    private static final ObjectMapper mapper = new ObjectMapper();
    private final RepoHighlightSessionDocument repoHighlightSessionDocument;
    private final RepoSubmissionOwner repoSubmissionOwner;
    private final RepoRelationSubmissionSession repoRelationSubmissionSession;
    private final RepoUser repoUser;
    private final RepoUserReference repoUserReference;
    private final RepoSubmission repoSubmission;
    private final RestTemplate moodleClient;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
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
                         RepoUser repoUser,
                         PasswordEncoder passwordEncoder,
                         AuthenticationManager authenticationManager,
                         RepoRelationSubmissionSession repoRelationSubmissionSession,
                         RepoSubmissionOwner repoSubmissionOwner,
                         ServiceHighlight serviceHighlight,
                         RepoElasticsearchDelete repoElasticsearchDelete,
                         RepoHighlightSessionDocument repoHighlightSessionDocument) {
        this.repoSubmission = repoSubmission;
        this.repoUserReference = repoUserReference;
        this.moodleClient = moodleClient;
        this.repoUser = repoUser;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.repoRelationSubmissionSession = repoRelationSubmissionSession;
        this.repoSubmissionOwner = repoSubmissionOwner;
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
                        } catch (UnsupportedLanguage e) {
                            //                            log.warn("[Service moodle] File {} is ignored because its language isn't supported",
                            //                                     zipEntry.getName());
                        }

                    }
                }
                if (!fileDocuments.isEmpty()) request.setSources(fileDocuments);
            }
        }

        return requests;
    }

    private static UriComponentsBuilder buildDefaultQueryParamsBuilder(String token, String function, String path) {
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
        } catch (UnsupportedLanguage e) {
            //            log.warn("[Service moodle] File {} is ignored because its language isn't supported", zipEntry.getName());
        }
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

    public UserDetails signin(SignInRequest request, HttpServletRequest httpServletRequest)
            throws JsonProcessingException {
        // Get token and userid from moodle
        Authentication authentication = getMoodleAccount(request);

        // Set context
        SecurityContextHolder.getContext().setAuthentication(authentication);
        httpServletRequest.getSession()
                          .setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                                        SecurityContextHolder.getContext());
        return (UserDetails) authentication.getPrincipal();
    }

    public Page<CourseDTO> getCourses(Pageable pageable) {
        UserImpl user = ServiceHighlight.getUserFromContext();
        if (user == null || user.getId() == null) {
            log.error("[Service moodle] Fail to get user information");
            throw new MoodleCourseException("Fail to get user information");
        }
        UserReference reference = repoUserReference.findFirstByInternalUserId(user.getId());
        ResponseEntity<JsonNode> entity = getCoursesOfUser(reference);
        List<CourseDTO> courses = CourseDTO.asList(Objects.requireNonNull(entity.getBody()));
        courses.sort(Comparator.comparing(CourseDTO::getId).reversed());
        return getListWithPageable(pageable, courses);
    }

    public AssignsOverviewDTO getAssigns(long courseId, Pageable pageable) {
        UserImpl user = ServiceHighlight.getUserFromContext();
        if (user == null || user.getId() == null) {
            log.error("[Service moodle] Fail to get user information");
            throw new MoodleCourseException("Fail to get user information");
        }
        UserReference reference = repoUserReference.findFirstByInternalUserId(user.getId());
        AssignsOverviewDTO overviewDTO = AssignsOverviewDTO.builder().build();
        List<AssignDTO> assigns = enrichAssignments(reference, courseId);
        assigns.sort(Comparator.comparing(AssignDTO::getId));
        overviewDTO.setCourse(enrichCourseDetail(courseId, reference));
        overviewDTO.setAssigns(getListWithPageable(pageable, assigns));
        return overviewDTO;
    }

    public Page<Submission.SubmissionDTO> getSubmissions(long courseId, long assignId, Pageable pageable) {
        UserImpl user = ServiceHighlight.getUserFromContext();
        if (user == null || user.getId() == null) {
            log.error("[Service moodle] Fail to get user information");
            throw new MoodleCourseException("Fail to get user information");
        }
        UserReference reference = repoUserReference.findFirstByInternalUserId(user.getId());
        List<Submission> submissions = getSubmissionsInCourse(courseId, new ArrayList<>(List.of(assignId)), reference);
        submissions = repoSubmission.saveAll(submissions);
        submissions.sort(Comparator.comparing(Submission::getId));
        if (pageable.getPageNumber() * pageable.getPageSize() > submissions.size())
            return new PageImpl<>(new ArrayList<>(), pageable, submissions.size());
        return new PageImpl<>(submissions.subList((int) pageable.getOffset(),
                                                  (int) Math.min(pageable.getPageSize() + pageable.getOffset(),
                                                                 submissions.size()))
                                         .stream()
                                         .map(Submission::toDTO)
                                         .toList(), pageable, submissions.size());
    }

    public MoodleResponse detectSubmissions(List<Long> submissionIds) throws IOException {
        UserImpl user = ServiceHighlight.getUserFromContext();
        if (user == null || user.getId() == null) {
            log.error("[Service moodle] Fail to get user information");
            throw new MoodleCourseException("Fail to get user information");
        }
        UserReference reference = repoUserReference.findFirstByInternalUserId(user.getId());
        List<HighlightSessionReportDTO> sessionReportDTOS = new ArrayList<>();
        // Get list of submissions
        List<Submission> submissions = repoSubmission.findAllByIdIn(submissionIds);
        // Classify submissions
        // They will be split into 3 groups:
        // + Modify: the submission has been detected, but new file are updated -> need overwrite session
        // + Add: the submission hasn't been detected yet -> detect
        // + Ignore: the submission has been detected, and there is no relation that has been updated -> ignore
        Map<String, List<RelationWithOwner>> mapRelationWithOwnerByNeed = classifySubmissions(submissions);
        sessionReportDTOS.addAll(handleNeedAddSubmissions(mapRelationWithOwnerByNeed.get("add"), reference));
        sessionReportDTOS.addAll(handleNeedModifySubmissions(mapRelationWithOwnerByNeed.get("modify"), reference));
        sessionReportDTOS.addAll(handleNeedIgnoreSubmissions(mapRelationWithOwnerByNeed.get("ignore")));
        repoSubmission.saveAll(submissions);
        return MoodleResponse.builder().data(sessionReportDTOS).build();
    }

    private List<AssignDTO> enrichAssignments(UserReference reference, long courseId) {
        UriComponentsBuilder builder = buildDefaultQueryParamsBuilder(reference.getToken(),
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

    private List<AssignDTO> fulfillAssignments(JsonNode data) {
        JsonNode moodleCourses = data.get("courses");
        List<AssignDTO> assigns = new ArrayList<>();
        moodleCourses.forEach(moodleCourse -> assigns.addAll(AssignDTO.from(moodleCourse.get("assignments"))));
        return assigns;
    }

    private List<Submission> getSubmissionsInCourse(long courseId, List<Long> assignIds, UserReference reference) {
        UriComponentsBuilder builder = buildDefaultQueryParamsBuilder(reference.getToken(),
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
                if (ownerId == reference.getReferenceUserId()) return;

                SubmissionOwner owner = repoSubmissionOwner.findFirstByReferenceOwnerId(ownerId);
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
        Map<Long, SubmissionOwner> mapOwnerBySubmissionReferenceId = getCourseSubmissionOwners(ownerIds,
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

    private Map<Long, SubmissionOwner> getCourseSubmissionOwners(Set<Long> ownerIds,
                                                                 Map<Long, List<Long>> mapListSubmissionIdByOwnerId,
                                                                 UserReference reference) {
        Map<Long, SubmissionOwner> mapOwnerBySubmissionReferenceId = new HashMap<>();
        List<SubmissionOwner> owners = getOwnersFromIds(ownerIds, reference);
        owners.forEach(owner -> mapListSubmissionIdByOwnerId.get(owner.getReferenceOwnerId())
                                                            .forEach(submissionId -> mapOwnerBySubmissionReferenceId.put(
                                                                    submissionId,
                                                                    owner)));
        return mapOwnerBySubmissionReferenceId;
    }

    private List<SubmissionOwner> getOwnersFromIds(Set<Long> ownerIds, UserReference reference) {
        UriComponentsBuilder builder = buildDefaultQueryParamsBuilder(reference.getToken(),
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

    private List<SubmissionOwner> parseSubmissionOwners(JsonNode data) {
        List<SubmissionOwner> owners = new ArrayList<>();
        data.forEach(owner -> owners.add(SubmissionOwner.builder()
                                                        .email(owner.get("email").asText())
                                                        .fullname(owner.get("fullname").asText())
                                                        .referenceOwnerId(owner.get("id").asLong())
                                                        .profileImageUrl(owner.get("profileimageurlsmall").asText())
                                                        .build()));

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
        UriComponentsBuilder builder = buildDefaultQueryParamsBuilder(reference.getToken(),
                                                                      MOODLE_GET_COURSES_FUNCTION,
                                                                      moodleWebServiceUri).queryParam("userid",
                                                                                                      reference.getReferenceUserId());
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
    private Authentication getMoodleAccount(SignInRequest request) throws JsonProcessingException {
        // Call moodle to get user_id and token
        UriComponentsBuilder getTokenParams = UriComponentsBuilder.fromPath(moodleSigninUri)
                                                                  .queryParam("username", request.getUsername())
                                                                  .queryParam("password", request.getPassword())
                                                                  .queryParam("service", webServiceName);
        String uri = moodleClient.getUriTemplateHandler().expand(getTokenParams.toUriString()).toString();
        String response = moodleClient.getForObject(uri, String.class);
        String token = mapper.readTree(response).get("token").asText();
        UserReference reference = repoUserReference.findFirstByToken(token);
        if (reference == null) {
            // Enrich user id by token
            UriComponentsBuilder getSiteInfoParams = UriComponentsBuilder.fromPath(moodleWebServiceUri)
                                                                         .queryParam("wstoken", token)
                                                                         .queryParam("wsfunction",
                                                                                     "core_webservice_get_site_info")
                                                                         .queryParam("moodlewsrestformat", "json");
            response = moodleClient.getForObject(moodleClient.getUriTemplateHandler()
                                                             .expand(getSiteInfoParams.toUriString())
                                                             .toString(), String.class);
            long referenceUserId = mapper.readTree(response).get("userid").asLong();
            reference = UserReference.builder().token(token).referenceUserId(referenceUserId).build();
        }
        // Create user if not exist
        UserImpl user;
        if (reference.getInternalUserId() == null) {
            // Create new user
            String encodePassword = passwordEncoder.encode(String.format("moodle@%s", token));
            user = repoUser.findUserImplByUsername(String.format("moodle@%s", request.getUsername()));
            if (user == null) user = repoUser.createOrgUser(UserImpl.builder()
                                                                    .username(String.format("moodle@%s",
                                                                                            request.getUsername()))
                                                                    .password(encodePassword)
                                                                    .build());
            reference.setInternalUserId(user.getId());
            repoUserReference.save(reference);
        } else {
            user = repoUser.getReferenceById(reference.getInternalUserId());
        }
        return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(),
                                                                                          String.format("moodle@%s",
                                                                                                        token)));
    }

    private CourseDTO enrichCourseDetail(long courseId, UserReference reference) {
        UriComponentsBuilder builder = buildDefaultQueryParamsBuilder(reference.getToken(),
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
        return CourseDTO.asDetail(Objects.requireNonNull(entity.getBody()).get("courses").get(0));
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
        List<HighlightSessionReportDTO> reportDTOS = new ArrayList<>();
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
            reportDTOS.add(detectSubmission(relation, reference));
        });
        // Perform deleting from ES index
        deleteDocumentsFromElasticsearch(pairIndexWithIds);
        // Clear all sessions and their files
        repoHighlightSessionDocument.deleteAll(sessions);
        return reportDTOS;
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
                                                               SubmissionOwner owner) {
        // Enrich zip file from moodle
        byte[] data = getSubmissionFileFromMoodle(file.getFileUri(), reference);
        List<FileDocument> documents = parseFileDocuments(file, data, owner);
        return HighlightSessionRequest.builder()
                                      .sessionName(FileSystemUtil.getFileName(file.getFilename()))
                                      .sources(documents)
                                      .build();
    }

    private List<FileDocument> parseFileDocuments(SubmissionFile file, byte[] data, SubmissionOwner owner) {
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
                } catch (UnsupportedLanguage e) {
                    //                    log.warn("[Service moodle] File {} is ignored because its language isn't supported", filename);
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
        SubmissionOwner owner;
        RelationSubmissionSession relation;
    }
}
