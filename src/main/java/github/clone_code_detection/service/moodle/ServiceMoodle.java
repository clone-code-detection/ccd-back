package github.clone_code_detection.service.moodle;


import com.fasterxml.jackson.databind.JsonNode;
import github.clone_code_detection.entity.authenication.UserImpl;
import github.clone_code_detection.entity.moodle.*;
import github.clone_code_detection.entity.moodle.dto.AssignDTO;
import github.clone_code_detection.entity.moodle.dto.CourseDTO;
import github.clone_code_detection.entity.moodle.dto.CourseOverviewDTO;
import github.clone_code_detection.entity.moodle.dto.MoodleLinkRequest;
import github.clone_code_detection.entity.query.QueryInstruction;
import github.clone_code_detection.exceptions.moodle.NotFoundException;
import github.clone_code_detection.exceptions.moodle.UnauthorizedException;
import github.clone_code_detection.exceptions.moodle.UnavailableException;
import github.clone_code_detection.repo.RepoMoodleUser;
import github.clone_code_detection.repo.RepoRelationSubmissionReport;
import github.clone_code_detection.repo.RepoSubmission;
import github.clone_code_detection.repo.RepoUserReference;
import github.clone_code_detection.service.user.ServiceAuthentication;
import github.clone_code_detection.util.TimeUtil;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.auth.AuthenticationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;

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
    private final ThreadPoolExecutor threadPoolExecutor;
    private final RepoMoodleUser repoMoodleUser;
    private final RepoRelationSubmissionReport repoRelationSubmissionReport;
    private final RepoSubmission repoSubmission;
    private final RestTemplate moodleClient;

    private final DetectSelectedSubmissionJobFactory detectSelectedSubmissionJobFactory;
    private final RepoUserReference repoUserReference;
    @Value("${moodle.web-service}")
    String webServiceName;
    @Value("${moodle.signin-uri}")
    String moodleSigninUri;
    @Value("${moodle.web-service-uri}")
    String moodleWebServiceUri;

    @Autowired
    public ServiceMoodle(ThreadPoolExecutor threadPoolExecutor,
                         RestTemplate moodleClient,
                         RepoSubmission repoSubmission,
                         RepoRelationSubmissionReport repoRelationSubmissionReport,
                         RepoMoodleUser repoMoodleUser,
                         DetectSelectedSubmissionJobFactory detectSelectedSubmissionJobFactory,
                         RepoUserReference userReference) {
        this.threadPoolExecutor = threadPoolExecutor;
        this.repoSubmission = repoSubmission;
        this.moodleClient = moodleClient;
        this.repoRelationSubmissionReport = repoRelationSubmissionReport;
        this.repoMoodleUser = repoMoodleUser;
        this.detectSelectedSubmissionJobFactory = detectSelectedSubmissionJobFactory;
        this.repoUserReference = userReference;
    }

    @NotNull
    private static <T> PageImpl<T> getListWithPageable(Pageable pageable, List<T> list) {
        if (pageable.getPageNumber() * pageable.getPageSize() >= list.size())
            return new PageImpl<>(Collections.emptyList(), pageable, list.size());
        return new PageImpl<>(list.subList((int) pageable.getOffset(),
                                           (int) Math.min(pageable.getPageSize() + pageable.getOffset(), list.size())),
                              pageable,
                              list.size());
    }

    @NotNull
    private UserImpl getUser() {
        UserImpl user = ServiceAuthentication.getUserFromContext();
        if (user == null || user.getId() == null) {
            log.error("[Service moodle] Fail to get user information");
            throw new UnavailableException("Fail to get user information");
        }
        if (user.getReference() == null) {
            UserReference reference = repoUserReference.findFirstByInternalUserId(user.getId());
            if (reference == null) {
                log.error("[Service moodle] No user reference");
                throw new UnauthorizedException("Fail to get moodle account");
            }
            user.setReference(reference);
        }
        return user;
    }

    private UriComponentsBuilder buildDefaultUriBuilder(UserReference reference, String function) {
        return buildDefaultUriBuilder(reference.getToken(), reference.getMoodleUrl(), function);
    }

    private UriComponentsBuilder buildDefaultUriBuilder(String token, String moodleUrl, String function) {
        return UriComponentsBuilder.fromUriString(moodleUrl)
                                   .path(moodleWebServiceUri)
                                   .queryParam("wstoken", token)
                                   .queryParam("wsfunction", function)
                                   .queryParam("moodlewsrestformat", "json");
    }

    public MoodleResponse linkCurrentUserToMoodleAccount(MoodleLinkRequest request) {
        // Get token and userid from moodle
        UserImpl user = ServiceAuthentication.getUserFromContext();
        if (user == null) {
            log.error("[Service moodle] Fail to get user information");
            throw new UnauthorizedException("Fail to get user information");
        }
        UserReference reference = repoUserReference.findFirstByInternalUserId(user.getId());
        if (reference != null) {
            user.setReference(reference);
            return MoodleResponse.builder().message("Moodle account already been linked").build();
        }
        reference = getMoodleAccount(request);
        reference.setInternalUser(user);
        reference = repoUserReference.save(reference);
        user.setReference(reference);
        return MoodleResponse.builder().message("Link to moodle account successfully").build();
    }

    public CourseOverviewDTO getCourseOverview(Pageable pageable) {
        UserImpl user = getUser();
        ResponseEntity<JsonNode> entity = getCoursesOfUser(user.getReference());
        List<Course> courses = Course.asList(Objects.requireNonNull(entity.getBody()));
        courses.sort(Comparator.comparing(Course::getId).reversed());
        return CourseOverviewDTO.builder()
                                .courses(getListWithPageable(pageable, courses))
                                .user(user.getReference().getReferenceUser().toDTO())
                                .build();
    }

    public CourseDTO getCourseDetail(long courseId, Pageable pageable) {
        UserImpl user = getUser();
        CourseDTO overviewDTO = CourseDTO.builder().build();
        List<Assign> assigns = enrichAssignments(user.getReference(), courseId);
        assigns.sort(Comparator.comparing(Assign::getId));
        overviewDTO.setCourse(enrichCourseDetail(courseId, user.getReference()));
        overviewDTO.setAssigns(getListWithPageable(pageable, assigns));
        return overviewDTO;
    }

    public AssignDTO getAssignDetail(long courseId, long assignId, Pageable pageable) {
        UserImpl user = getUser();
        AssignDTO assignDTO = AssignDTO.builder().build();
        Assign assign = enrichAssignments(user.getReference(), courseId).stream()
                                                                        .filter(e -> e.getId() == assignId)
                                                                        .findFirst()
                                                                        .orElse(null);
        if (assign == null) throw new NotFoundException("Assignment not found");
        List<Submission> submissions = getSubmissionsInCourse(courseId, assign.getCourseName(), assign.getName(),
                                                              List.of(assignId), user.getReference());
        submissions = repoSubmission.saveAll(submissions);
        submissions.sort(Comparator.comparing(Submission::getId));
        assignDTO.setAssign(assign);
        assignDTO.setSubmissions(getListWithPageable(pageable, submissions.stream().map(Submission::toDTO).toList()));
        return assignDTO;
    }

    public MoodleResponse detectSelectedSubmissions(@NotNull List<Long> submissionIds,
                                                    @NotNull QueryInstruction queryInstruction) throws
                                                                                                AuthenticationException {
        UserImpl user = getUser();
        if (user.getReference() == null) {
            log.error("[Service moodle] User info not found");
            throw new AuthenticationException("User not found");
        }
        DetectSelectedSubmissionJob.DetectSelectedSubmissionJobBuilder builder = DetectSelectedSubmissionJob.builder()
                                                                                                            .queryInstruction(
                                                                                                                    queryInstruction)
                                                                                                            .submissionIds(
                                                                                                                    submissionIds)
                                                                                                            .user(user);
        DetectSelectedSubmissionJob detectSelectedSubmissionJob = detectSelectedSubmissionJobFactory.newInstance(
                builder);
        threadPoolExecutor.submit(detectSelectedSubmissionJob);
        return MoodleResponse.builder().message("Receive detect signal successfully").build();
    }

    private List<Assign> enrichAssignments(UserReference reference, long courseId) {
        UriComponentsBuilder builder = buildDefaultUriBuilder(reference, MOODLE_GET_ASSIGNS_FUNCTION);
        builder.queryParam("courseids[]", courseId);
        ResponseEntity<JsonNode> entity = moodleClient.getForEntity(
                moodleClient.getUriTemplateHandler().expand(builder.toUriString()).toString(), JsonNode.class);
        if (!entity.getStatusCode().is2xxSuccessful()) {
            log.error("[Service moodle] Fail to get assigns by course id");
            throw new UnavailableException("Fail to enrich assignments from course id");
        }

        return fulfillAssignments(Objects.requireNonNull(entity.getBody()));
    }

    private List<Assign> fulfillAssignments(JsonNode data) {
        JsonNode moodleCourses = data.get("courses");
        List<Assign> assigns = new ArrayList<>();
        moodleCourses.forEach(moodleCourse -> assigns.addAll(Assign.from(moodleCourse)));
        return assigns;
    }

    private List<Submission> getSubmissionsInCourse(long courseId, String courseName, String assignName,
                                                    List<Long> assignIds, UserReference reference) {
        UriComponentsBuilder builder = buildDefaultUriBuilder(reference, MOODLE_GET_SUBMISSIONS_FUNCTION);
        assignIds.forEach(assignId -> builder.queryParam("assignmentids[]", assignId));
        ResponseEntity<JsonNode> entity = moodleClient.getForEntity(
                moodleClient.getUriTemplateHandler().expand(builder.toUriString()).toString(), JsonNode.class);
        if (!entity.getStatusCode().is2xxSuccessful()) {
            log.error("[Service moodle] Fail to get submissions by assignments");
            throw new UnavailableException("Fail to enrich submissions");
        }
        return parseSubmissions(Objects.requireNonNull(entity.getBody()).get("assignments"), courseId, courseName,
                                assignName, reference);
    }

    private List<Submission> parseSubmissions(@NotNull JsonNode assignments, long courseId, String courseName,
                                              String assignName, UserReference reference) {
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
                Submission submission = repoSubmission.findFirstByReferenceSubmissionId(submissionId);
                List<RelationSubmissionReport> relations = getSubmissionRelations(inputSubmission.get("plugins"));
                if (submission == null) {
                    // Case new submission
                    submission = Submission.builder()
                                           .referenceSubmissionId(submissionId)
                                           .createdAt(TimeUtil.parseZoneDateTime(
                                                   inputSubmission.get("timecreated").asLong()))
                                           .updatedAt(TimeUtil.parseZoneDateTime(
                                                   inputSubmission.get("timemodified").asLong()))
                                           .courseId(courseId)
                                           .assignId(assignId)
                                           .courseName(courseName)
                                           .assignName(assignName)
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

    private void updateSubmissionRelations(Submission submission, List<RelationSubmissionReport> newRelations) {
        List<RelationSubmissionReport> currentRelations = submission.getRelations();
        // Modified current relation that still exists in new relation
        List<RelationSubmissionReport> needDeleteRelations = new ArrayList<>();
        List<RelationSubmissionReport> finalRelations = new ArrayList<>();
        Map<String, RelationSubmissionReport> mapNewRelationByIndex = new HashMap<>();
        // Insert for new that is not found in current
        newRelations.forEach(newRelation -> {
            if (currentRelations.stream()
                                .map(RelationSubmissionReport::getFile)
                                .map(SubmissionFile::getFilename)
                                .noneMatch(currentFilename -> currentFilename.equals(newRelation.getFile()
                                                                                                .getFilename()))) {
                // Insert
                finalRelations.add(newRelation);
            }
            mapNewRelationByIndex.put(newRelation.getFile().getFilename(), newRelation);
        });
        currentRelations.forEach(currentRelation -> {
            RelationSubmissionReport newRelation = mapNewRelationByIndex.get(currentRelation.getFile().getFilename());
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
        repoRelationSubmissionReport.deleteAll(needDeleteRelations);
        submission.setRelations(finalRelations);
    }

    private Map<Long, MoodleUser> getCourseSubmissionOwners(Set<Long> ownerIds,
                                                            Map<Long, List<Long>> mapListSubmissionIdByOwnerId,
                                                            UserReference reference) {
        Map<Long, MoodleUser> mapOwnerBySubmissionReferenceId = new HashMap<>();
        List<MoodleUser> owners = getOwnersFromIds(ownerIds, reference);
        owners.forEach(owner -> mapListSubmissionIdByOwnerId.get(owner.getReferenceUserId())
                                                            .forEach(submissionId -> mapOwnerBySubmissionReferenceId.put(
                                                                    submissionId, owner)));
        return mapOwnerBySubmissionReferenceId;
    }

    private List<MoodleUser> getOwnersFromIds(Set<Long> ownerIds, UserReference reference) {
        UriComponentsBuilder builder = buildDefaultUriBuilder(reference, MOODLE_GET_OWNERS_FUNCTION).queryParam("field",
                                                                                                                "id");
        ownerIds.forEach(id -> builder.queryParam("values[]", id));
        ResponseEntity<JsonNode> entity = moodleClient.getForEntity(
                moodleClient.getUriTemplateHandler().expand(builder.toUriString()), JsonNode.class);
        if (!entity.getStatusCode().is2xxSuccessful()) {
            log.error("[Service moodle] Fail to enrich submission owners");
            throw new UnavailableException("Fail to enrich submission owners");
        }
        return parseSubmissionOwners(Objects.requireNonNull(entity.getBody()));
    }

    private List<MoodleUser> parseSubmissionOwners(JsonNode data) {
        List<MoodleUser> owners = new ArrayList<>();
        data.forEach(owner -> owners.add(MoodleUser.from(owner)));

        return owners;
    }

    private List<RelationSubmissionReport> getSubmissionRelations(JsonNode plugins) {
        List<RelationSubmissionReport> relationSubmissionReports = new ArrayList<>();
        plugins.forEach(plugin -> {
            if (plugin.get("type").asText().equals("file")) {
                plugin.get("fileareas").forEach(fileareas -> {
                    if (fileareas.get("area").asText().equals("submission_files")) {
                        fileareas.get("files").forEach(file -> {
                            SubmissionFile submissionFile = SubmissionFile.builder()
                                                                          .filename(file.get("filename").asText())
                                                                          .fileUri(file.get("fileurl").asText())
                                                                          .mimetype(file.get("mimetype").asText())
                                                                          .updatedAt(TimeUtil.parseZoneDateTime(
                                                                                  file.get("timemodified").asLong()))
                                                                          .build();
                            relationSubmissionReports.add(
                                    RelationSubmissionReport.builder().file(submissionFile).build());
                        });
                    }
                });
            }
        });
        return relationSubmissionReports;
    }

    @NotNull
    private ResponseEntity<JsonNode> getCoursesOfUser(UserReference reference) {
        UriComponentsBuilder builder = buildDefaultUriBuilder(reference, MOODLE_GET_COURSES_FUNCTION).queryParam(
                "userid", reference.getReferenceUser().getReferenceUserId());
        ResponseEntity<JsonNode> entity = moodleClient.getForEntity(
                moodleClient.getUriTemplateHandler().expand(builder.toUriString()).toString(), JsonNode.class);
        if (!entity.getStatusCode().is2xxSuccessful()) {
            log.error("[Service moodle] Fail to get courses from Moodle");
            throw new UnavailableException("Fail to get courses from Moodle");
        }
        return entity;
    }

    @NotNull
    private UserReference getMoodleAccount(MoodleLinkRequest request) {
        // Call moodle to get user_id and token
        UriComponentsBuilder getTokenParams = UriComponentsBuilder.fromUriString(request.getMoodleUrl())
                                                                  .path(moodleSigninUri)
                                                                  .queryParam("username", request.getUsername())
                                                                  .queryParam("password", request.getPassword())
                                                                  .queryParam("service", webServiceName);
        ResponseEntity<JsonNode> entity = moodleClient.getForEntity(
                moodleClient.getUriTemplateHandler().expand(getTokenParams.toUriString()).toString(), JsonNode.class);
        if (!entity.getStatusCode().is2xxSuccessful() || entity.getBody() == null) {
            log.error("[Service moodle] Can sign in by moodle account");
            throw new UnavailableException("Fail to signin by moodle account " + request.getUsername());
        }
        String token = entity.getBody().get("token").asText();
        // Enrich user id by token
        UriComponentsBuilder getSiteInfoParams = buildDefaultUriBuilder(token,
                                                                        request.getMoodleUrl(),
                                                                        MOODLE_GET_SITE_INFO_FUNCTION);
        entity = moodleClient.getForEntity(getSiteInfoParams.toUriString(), JsonNode.class);
        if (!entity.getStatusCode().is2xxSuccessful() || entity.getBody() == null) {
            log.error("[Service moodle] Can enrich user info by moodle account");
            throw new UnavailableException("Fail to enrich user info by moodle account " + request.getUsername());
        }
        long referenceUserId = entity.getBody().get("userid").asLong();
        MoodleUser userInfo = repoMoodleUser.findFirstByReferenceUserId(referenceUserId);
        if (userInfo == null) {
            // Enrich user info from user id
            UriComponentsBuilder enrichUserInfoParams = buildDefaultUriBuilder(token,
                                                                               request.getMoodleUrl(),
                                                                               MOODLE_GET_OWNERS_FUNCTION).queryParam(
                    "field", "id").queryParam("values[]", referenceUserId);
            entity = moodleClient.getForEntity(enrichUserInfoParams.toUriString(), JsonNode.class);
            if (!entity.getStatusCode().is2xxSuccessful()) {
                log.error("[Service moodle] Fail to enrich submission owners");
                throw new UnavailableException("Fail to enrich submission owners");
            }
            userInfo = MoodleUser.from(Objects.requireNonNull(entity.getBody()).get(0));
        }
        return UserReference.builder().token(token).moodleUrl(request.getMoodleUrl()).referenceUser(userInfo).build();
    }

    private Course enrichCourseDetail(long courseId, UserReference reference) {
        UriComponentsBuilder builder = buildDefaultUriBuilder(reference, MOODLE_GET_COURSE_DETAIL_FUNCTION);
        builder.queryParam("field", "id");
        builder.queryParam("value", courseId);
        ResponseEntity<JsonNode> entity = moodleClient.getForEntity(
                moodleClient.getUriTemplateHandler().expand(builder.toUriString()), JsonNode.class);
        if (!entity.getStatusCode().is2xxSuccessful()) {
            log.error("[Service moodle] Fail to enrich course detail content");
            throw new UnavailableException("Fail to enrich course detail");
        }
        return Course.asDetail(Objects.requireNonNull(entity.getBody()).get("courses").get(0));
    }

}
