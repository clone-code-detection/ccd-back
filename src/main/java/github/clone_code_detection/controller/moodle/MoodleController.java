package github.clone_code_detection.controller.moodle;

import com.fasterxml.jackson.core.JsonProcessingException;
import github.clone_code_detection.controller.authen.AuthenticationController;
import github.clone_code_detection.entity.authenication.SignInRequest;
import github.clone_code_detection.entity.highlight.report.HighlightSessionReportDTO;
import github.clone_code_detection.entity.highlight.request.HighlightSessionRequest;
import github.clone_code_detection.entity.index.IndexInstruction;
import github.clone_code_detection.entity.moodle.*;
import github.clone_code_detection.service.highlight.ServiceHighlight;
import github.clone_code_detection.service.moodle.ServiceMoodle;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
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
        Collection<HighlightSessionRequest> requests = ServiceMoodle.unzipMoodleFileAndGetRequests(source);
        Collection<HighlightSessionReportDTO> reports = new ArrayList<>();
        requests.forEach(request -> reports.add(serviceHighlight.createHighlightSession(request,
                                                                                        IndexInstruction.getDefaultInstruction())));
        return MoodleResponse.builder().message("OK").data(reports).build();
    }

    @PostMapping(path = "/signin", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public AuthenticationController.UserAuthenticateResponse signinWithMoodleAccount(@Validated SignInRequest request,
                                                                                     HttpServletRequest httpServletRequest)
            throws JsonProcessingException {
        assert request != null : new RuntimeException("Invalid request");
        UserDetails userDetails = serviceMoodle.signin(request, httpServletRequest);
        return new AuthenticationController.UserAuthenticateResponse(userDetails);
    }

    @GetMapping(path = "/courses")
    @ResponseStatus(HttpStatus.OK)
    public Page<CourseDTO> getCourses(@PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable)
            throws JsonProcessingException {
        return serviceMoodle.getCourses(pageable);
    }

    @GetMapping(path = "/assigns")
    @ResponseStatus(HttpStatus.OK)
    public Page<AssignDTO> getAssigns(@RequestParam("course_id") long courseId,
                                      @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable)
            throws JsonProcessingException {
        return serviceMoodle.getAssigns(courseId, pageable);
    }

    @GetMapping(path = "/submissions")
    @ResponseStatus(HttpStatus.OK)
    public Page<Submission.SubmissionDTO> getSubmissions(@RequestParam("course_id") long courseId,
                                                         @RequestParam("assign_id") long assignId,
                                                         @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable)
            throws JsonProcessingException {
        return serviceMoodle.getSubmissions(courseId, assignId, pageable);
    }

    @PostMapping(path = "/detect-submissions")
    @ResponseStatus(HttpStatus.OK)
    public MoodleResponse detectSelectedSubmissions(@RequestBody DetectRequest request) {
        return MoodleResponse.builder().build();
    }
}
