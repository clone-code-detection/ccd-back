package github.clone_code_detection.controller.moodle;

import github.clone_code_detection.entity.highlight.dto.SimilarityReportInfoDTO;
import github.clone_code_detection.entity.highlight.request.SimilarityDetectRequest;
import github.clone_code_detection.entity.index.IndexInstruction;
import github.clone_code_detection.entity.moodle.DetectRequest;
import github.clone_code_detection.entity.moodle.MoodleResponse;
import github.clone_code_detection.entity.moodle.dto.AssignDTO;
import github.clone_code_detection.entity.moodle.dto.CourseDTO;
import github.clone_code_detection.entity.moodle.dto.CourseOverviewDTO;
import github.clone_code_detection.entity.moodle.dto.MoodleLinkRequest;
import github.clone_code_detection.service.highlight.ServiceHighlight;
import github.clone_code_detection.service.moodle.ServiceMoodle;
import org.apache.http.auth.AuthenticationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

@RestController
@RequestMapping("/api/moodle")
public class MoodleController {
    private final ServiceHighlight serviceHighlight;
    private final ServiceMoodle serviceMoodle;

    @Autowired
    public MoodleController(ServiceHighlight serviceHighlight, ServiceMoodle serviceMoodle) {
        this.serviceHighlight = serviceHighlight;
        this.serviceMoodle = serviceMoodle;
    }


    @PostMapping(path = "/detect", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public MoodleResponse createHighlightSessionForMoodle(@RequestParam("source") MultipartFile source)
            throws IOException {
        // moodle source is a zip file that contains multiple folders.
        // Each folder is list of file submissions of one student.
        // For each file in student's folder, we consider it as one highlight session.
        Collection<SimilarityDetectRequest> requests = ServiceMoodle.unzipMoodleFileAndGetRequests(source);
        Collection<SimilarityReportInfoDTO> reports = new ArrayList<>();
        requests.forEach(request -> reports.add(SimilarityReportInfoDTO.from(serviceHighlight.createSimilarityReport(
                request,
                IndexInstruction.getDefaultInstruction()))));
        return MoodleResponse.builder().message("OK").data(reports).build();
    }

    @PostMapping(path = "/signin", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public MoodleResponse signinWithMoodleAccount(@Validated MoodleLinkRequest request) throws AuthenticationException {
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

    @PostMapping(path = "/detect-submissions")
    @ResponseStatus(HttpStatus.OK)
    public MoodleResponse detectSelectedSubmissions(@RequestBody DetectRequest request) {
        return serviceMoodle.detectSelectedSubmissions(request.getSubmissionIds());
    }
}
