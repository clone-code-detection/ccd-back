package github.clone_code_detection.service.moodle;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import github.clone_code_detection.entity.authenication.SignInRequest;
import github.clone_code_detection.entity.authenication.UserImpl;
import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.entity.highlight.request.HighlightSessionRequest;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
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
    private static final String MOODLE_GET_COURSE_FUNCTION = "core_enrol_get_users_courses";
    private static final String MOODLE_GET_ASSIGNS_FUNCTION = "mod_assign_get_assignments";
    private static final String MOODLE_GET_SUBMISSIONS_FUNCTION = "mod_assign_get_submissions";
    private static final String MOODLE_GET_OWNERS_FUNCTION = "core_user_get_users_by_field";
    private static final LanguageUtil languageUtil = LanguageUtil.getInstance();
    private static final ObjectMapper mapper = new ObjectMapper();
    private final RepoSubmissionOwner repoSubmissionOwner;
    private final RepoRelationSubmissionSession repoRelationSubmissionSession;
    private final RepoUser repoUser;
    private final RepoUserReference repoUserReference;
    private final RepoSubmission repoSubmission;
    private final RestTemplate moodleClient;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
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
                         RepoSubmissionOwner repoSubmissionOwner) {
        this.repoSubmission = repoSubmission;
        this.repoUserReference = repoUserReference;
        this.moodleClient = moodleClient;
        this.repoUser = repoUser;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.repoRelationSubmissionSession = repoRelationSubmissionSession;
        this.repoSubmissionOwner = repoSubmissionOwner;
    }

    public static UriComponentsBuilder buildDefaultQueryParamsBuilder(String token, String function, String path) {
        return UriComponentsBuilder.fromPath(path)
                                   .queryParam("wstoken", token)
                                   .queryParam("wsfunction", function)
                                   .queryParam("moodlewsrestformat", "json");
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
                HighlightSessionRequest request = getExistRequestOrCreateNew(requests,
                                                                             sessionName); // get the exists highlight session request or new request which has been added into Collection

                Collection<FileDocument> fileDocuments = new ArrayList<>();
                zipInputStream.transferTo(byteArrayOutputStream);
                switch (FilenameUtils.getExtension(zipEntry.getName())) {
                    case "" -> log.warn("[Service moodle] File {} has empty extension", zipEntry.getName());
                    // Case nested zip file inside root file
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
                            log.warn("[Service moodle] File {} is ignored because its language isn't supported",
                                     zipEntry.getName());
                        }

                    }
                }
                if (!fileDocuments.isEmpty()) request.setSources(fileDocuments);
            }
        }

        return requests;
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
            log.warn("[Service moodle] File {} is ignored because its language isn't supported", zipEntry.getName());
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

    public Page<CourseDTO> getCourses(Pageable pageable) throws JsonProcessingException {
        UserImpl user = ServiceHighlight.getUserFromContext();
        if (user == null || user.getId() == null) {
            log.error("[Service moodle] Fail to get user information");
            throw new MoodleCourseException("Fail to get user information");
        }
        UserReference reference = repoUserReference.findByInternalUserId(user.getId());
        ResponseEntity<String> coursesEntity = getCoursesOfUser(reference);
        JsonNode moodleCourses = mapper.readTree(coursesEntity.getBody());
        List<CourseDTO> courses = CourseDTO.from(moodleCourses);
        courses.sort(Comparator.comparing(CourseDTO::getId).reversed());
        return new PageImpl<>(courses.subList((int) pageable.getOffset(),
                                              (int) Math.min(pageable.getPageSize() + pageable.getOffset(),
                                                             courses.size())), pageable, courses.size());
    }

    private List<AssignDTO> enrichAssignments(UserReference reference, long courseId) throws JsonProcessingException {
        ResponseEntity<String> entity;
        UriComponentsBuilder builder = buildDefaultQueryParamsBuilder(reference.getToken(),
                                                                      MOODLE_GET_ASSIGNS_FUNCTION,
                                                                      moodleWebServiceUri);
        builder.queryParam("courseids[]", courseId);
        entity = moodleClient.getForEntity(moodleClient.getUriTemplateHandler()
                                                       .expand(builder.toUriString())
                                                       .toString(), String.class);
        if (!entity.getStatusCode().is2xxSuccessful()) {
            log.error("[Service moodle] Fail to get assigns by course id");
            throw new MoodleAssignmentException("Fail to enrich assignments from course id");
        }

        JsonNode moodleAssigns = mapper.readTree(entity.getBody());
        return fulfillAssignments(moodleAssigns);
    }

    private List<AssignDTO> fulfillAssignments(JsonNode data) {
        JsonNode moodleCourses = data.get("courses");
        List<AssignDTO> assigns = new ArrayList<>();
        moodleCourses.forEach(moodleCourse -> assigns.addAll(AssignDTO.from(moodleCourse.get("assignments"))));
        return assigns;
    }

    private List<Submission> getSubmissionsInCourse(long courseId, List<Long> assignIds, UserReference reference)
            throws JsonProcessingException {
        UriComponentsBuilder builder = buildDefaultQueryParamsBuilder(reference.getToken(),
                                                                      MOODLE_GET_SUBMISSIONS_FUNCTION,
                                                                      moodleWebServiceUri);
        assignIds.forEach(assignId -> builder.queryParam("assignmentids[]", assignId));
        ResponseEntity<String> entity = moodleClient.getForEntity(moodleClient.getUriTemplateHandler()
                                                                              .expand(builder.toUriString())
                                                                              .toString(), String.class);
        if (!entity.getStatusCode().is2xxSuccessful()) {
            log.error("[Service moodle] Fail to get submissions by assignments");
            throw new MoodleSubmissionException("Fail to enrich submissions");
        }
        JsonNode data = mapper.readTree(entity.getBody());
        return parseSubmissions(data.get("assignments"), courseId, reference);
    }

    private List<Submission> parseSubmissions(@NotNull JsonNode assignments, long courseId, UserReference reference)
            throws JsonProcessingException {
        List<Submission> submissions = new ArrayList<>();
        Set<Long> ownerIds = new HashSet<>();
        Map<Long, List<Long>> mapListSubmissionIdByOwnerId = new HashMap<>();
        assignments.forEach(assignment -> {
            long assignId = assignment.get("assignmentid").asLong();
            assignment.get("submissions").forEach(inputSubmission -> {
                long ownerId = inputSubmission.get("userid").asLong();
                long submissionId = inputSubmission.get("id").asLong();
                if (ownerId == reference.getReferenceUserId()) return;

                SubmissionOwner owner = repoSubmissionOwner.findByReferenceOwnerId(ownerId);
                if (owner == null) {
                    ownerIds.add(ownerId);
                    if (mapListSubmissionIdByOwnerId.containsKey(ownerId))
                        mapListSubmissionIdByOwnerId.get(ownerId).add(submissionId);
                    else mapListSubmissionIdByOwnerId.put(ownerId, new ArrayList<>(List.of(submissionId)));
                }
                Submission submission = repoSubmission.findByReferenceSubmissionId(inputSubmission.get("id").asLong());
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
        currentRelations.forEach(currentRelation -> {
            // If current found in new then modify the submission file
            for (RelationSubmissionSession newRelation : newRelations) {
                if (newRelation.getFile().getFilename().equals(currentRelation.getFile().getFilename())) {
                    // Update
                    currentRelation.getFile().setUpdatedAt(newRelation.getFile().getUpdatedAt());
                    currentRelation.getFile().setFileUri(newRelation.getFile().getFileUri());
                    finalRelations.add(currentRelation);
                    return;
                }
            }
            // Case current not found in new then delete
            needDeleteRelations.add(currentRelation);
        });
        repoRelationSubmissionSession.deleteAll(needDeleteRelations);
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
        });
        submission.setRelations(finalRelations);
    }

    private Map<Long, SubmissionOwner> getCourseSubmissionOwners(Set<Long> ownerIds,
                                                                 Map<Long, List<Long>> mapListSubmissionIdByOwnerId,
                                                                 UserReference reference)
            throws JsonProcessingException {
        Map<Long, SubmissionOwner> mapOwnerBySubmissionReferenceId = new HashMap<>();
        List<SubmissionOwner> owners = getOwnersFromIds(ownerIds, reference);
        owners.forEach(owner -> mapListSubmissionIdByOwnerId.get(owner.getReferenceOwnerId())
                                                            .forEach(submissionId -> mapOwnerBySubmissionReferenceId.put(
                                                                    submissionId,
                                                                    owner)));
        return mapOwnerBySubmissionReferenceId;
    }

    private List<SubmissionOwner> getOwnersFromIds(Set<Long> ownerIds, UserReference reference)
            throws JsonProcessingException {
        UriComponentsBuilder builder = buildDefaultQueryParamsBuilder(reference.getToken(),
                                                                      MOODLE_GET_OWNERS_FUNCTION,
                                                                      moodleWebServiceUri).queryParam("field", "id");
        ownerIds.forEach(id -> builder.queryParam("values[]", id));
        ResponseEntity<String> entity = moodleClient.getForEntity(moodleClient.getUriTemplateHandler()
                                                                              .expand(builder.toUriString()),
                                                                  String.class);
        if (!entity.getStatusCode().is2xxSuccessful()) {
            log.error("[Service moodle] Fail to enrich submission owners");
            throw new MoodleSubmissionException("Fail to enrich submission owners");
        }
        JsonNode data = mapper.readTree(entity.getBody());
        return parseSubmissionOwners(data);
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
                            RelationSubmissionSession relation;
                            SubmissionFile submissionFile = SubmissionFile.builder()
                                                                          .filename(file.get("filename").asText())
                                                                          .fileUri(file.get("fileurl").asText())
                                                                          .mimetype(file.get("mimetype").asText())
                                                                          .updatedAt(TimeUtil.parseZoneDateTime(file.get(
                                                                                  "timemodified").asLong()))
                                                                          .build();
                            relation = RelationSubmissionSession.builder().file(submissionFile).build();
                            relationSubmissionSessions.add(relation);
                        });
                    }
                });
            }
        });
        return relationSubmissionSessions;
    }

    @NotNull
    private ResponseEntity<String> getCoursesOfUser(UserReference reference) {
        UriComponentsBuilder builder = buildDefaultQueryParamsBuilder(reference.getToken(),
                                                                      MOODLE_GET_COURSE_FUNCTION,
                                                                      moodleWebServiceUri).queryParam("userid",
                                                                                                      reference.getReferenceUserId());
        ResponseEntity<String> entity = moodleClient.getForEntity(moodleClient.getUriTemplateHandler()
                                                                              .expand(builder.toUriString())
                                                                              .toString(), String.class);
        if (!entity.getStatusCode().is2xxSuccessful()) {
            log.error("[Service moodle] Fail to get courses from Moodle");
            throw new MoodleCourseException("Fail to get courses from Moodle");
        }
        return entity;
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
        UserReference reference = repoUserReference.findByToken(token);
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
            user = repoUser.createOrgUser(UserImpl.builder()
                                                  .username(String.format("moodle@%s", request.getUsername()))
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

    public Page<AssignDTO> getAssigns(long courseId, Pageable pageable) throws JsonProcessingException {
        UserImpl user = ServiceHighlight.getUserFromContext();
        if (user == null || user.getId() == null) {
            log.error("[Service moodle] Fail to get user information");
            throw new MoodleCourseException("Fail to get user information");
        }
        UserReference reference = repoUserReference.findByInternalUserId(user.getId());
        List<AssignDTO> assigns = enrichAssignments(reference, courseId);
        assigns.sort(Comparator.comparing(AssignDTO::getId));
        return new PageImpl<>(assigns.subList((int) pageable.getOffset(),
                                              (int) Math.min(pageable.getPageSize() + pageable.getOffset(),
                                                             assigns.size())), pageable, assigns.size());
    }

    public Page<Submission.SubmissionDTO> getSubmissions(long courseId, long assignId, Pageable pageable)
            throws JsonProcessingException {
        UserImpl user = ServiceHighlight.getUserFromContext();
        if (user == null || user.getId() == null) {
            log.error("[Service moodle] Fail to get user information");
            throw new MoodleCourseException("Fail to get user information");
        }
        UserReference reference = repoUserReference.findByInternalUserId(user.getId());
        List<Submission> submissions = getSubmissionsInCourse(courseId, new ArrayList<>(List.of(assignId)), reference);
        submissions = repoSubmission.saveAll(submissions);
        submissions.sort(Comparator.comparing(Submission::getId));
        return new PageImpl<>(submissions.subList((int) pageable.getOffset(),
                                                  (int) Math.min(pageable.getPageSize() + pageable.getOffset(),
                                                                 submissions.size()))
                                         .stream()
                                         .map(Submission::toDTO)
                                         .toList(), pageable, submissions.size());
    }
}
