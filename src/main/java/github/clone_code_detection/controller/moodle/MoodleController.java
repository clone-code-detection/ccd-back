package github.clone_code_detection.controller.moodle;

import github.clone_code_detection.entity.moodle.DetectRequest;
import github.clone_code_detection.entity.moodle.MoodleResponse;
import github.clone_code_detection.entity.moodle.dto.AssignDTO;
import github.clone_code_detection.entity.moodle.dto.CourseDTO;
import github.clone_code_detection.entity.moodle.dto.CourseOverviewDTO;
import github.clone_code_detection.entity.moodle.dto.MoodleLinkRequest;
import github.clone_code_detection.entity.query.QueryInstruction;
import github.clone_code_detection.service.moodle.ServiceMoodle;
import org.apache.http.auth.AuthenticationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/moodle")
public class MoodleController {
    private final ServiceMoodle serviceMoodle;

    @Autowired
    public MoodleController(ServiceMoodle serviceMoodle) {
        this.serviceMoodle = serviceMoodle;
    }

    @PostMapping(path = "/signin", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public MoodleResponse signinWithMoodleAccount(@Validated MoodleLinkRequest request) {
        assert request != null : new RuntimeException("Invalid request");
        return serviceMoodle.linkCurrentUserToMoodleAccount(request);
    }

    @GetMapping(path = "/courses")
    @ResponseStatus(HttpStatus.OK)
    public CourseOverviewDTO getCourses(@PageableDefault Pageable pageable) {
        return serviceMoodle.getCourseOverview(pageable);
    }

    @GetMapping(path = "/course")
    @ResponseStatus(HttpStatus.OK)
    public CourseDTO getCourseDetail(@RequestParam("course_id") long courseId, @PageableDefault Pageable pageable) {
        return serviceMoodle.getCourseDetail(courseId, pageable);
    }

    @GetMapping(path = "/assign")
    @ResponseStatus(HttpStatus.OK)
    public AssignDTO getAssignDetail(@RequestParam("course_id") long courseId,
                                     @RequestParam("assign_id") long assignId,
                                     @PageableDefault Pageable pageable) {
        return serviceMoodle.getAssignDetail(courseId, assignId, pageable);
    }

    @PostMapping(path = "/detect-submissions", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public MoodleResponse detectSelectedSubmissions(@RequestParam(value = "submission_ids") List<Long> ids,
                                                    @RequestParam(value = "type", required = false, defaultValue = "1")
                                                    Integer type,
                                                    @RequestParam(value = "minimum_should_match",
                                                                  required = false,
                                                                  defaultValue = "70%") String minimumShouldMatch)
            throws AuthenticationException {
        DetectRequest request = DetectRequest.builder().submissionIds(ids).build();
        QueryInstruction queryInstruction = QueryInstruction.builder()
                                                            .minimumShouldMatch(minimumShouldMatch)
                                                            .type(type)
                                                            .build();
        return serviceMoodle.detectSelectedSubmissions(request.getSubmissionIds(), queryInstruction);
    }
}
