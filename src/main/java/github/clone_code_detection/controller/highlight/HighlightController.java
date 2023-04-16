package github.clone_code_detection.controller.highlight;

import github.clone_code_detection.entity.highlight.document.HighlightSessionDocument;
import github.clone_code_detection.entity.highlight.report.HighlightSessionReportDTO;
import github.clone_code_detection.entity.highlight.report.HighlightSingleSourceDTO;
import github.clone_code_detection.entity.highlight.report.HighlightSingleTargetMatchDTO;
import github.clone_code_detection.entity.index.IndexInstruction;
import github.clone_code_detection.service.highlight.ServiceHighlight;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;


@RestController
@RequestMapping("/api/highlight")
public class HighlightController {
    private final ServiceHighlight serviceHighlightTest;

    @Autowired
    public HighlightController(ServiceHighlight serviceHighlightTest) {this.serviceHighlightTest = serviceHighlightTest;}

    @RequestMapping(path = "/query", method = RequestMethod.POST, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public HighlightSessionReportDTO queryDocument(@RequestParam("source") MultipartFile source) {

        return serviceHighlightTest.highlight(source, IndexInstruction.getDefaultInstruction());
    }

    @RequestMapping(path = "/get-session-by-id/{id}", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public HighlightSessionReportDTO getSessionById(@PathVariable(name = "id") String id) {
        return serviceHighlightTest.getHighlightSessionById(id);
    }

    @RequestMapping(path = "/all-session", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Collection<HighlightSessionDocument.HighlightSessionProjection> getAllSession() {
        return serviceHighlightTest.getAllSession();
    }

    @RequestMapping(path = "/get-match-source/{id}", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public HighlightSingleSourceDTO getSingeSourceById(@PathVariable(name = "id") String id) {
        return serviceHighlightTest.getSingleSourceMatchById(id);
    }

    @RequestMapping(path = "/get-match-target/{id}", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public HighlightSingleTargetMatchDTO getSingleTargetById(@PathVariable(name = "id") String id) {
        return serviceHighlightTest.getSingleTargetMatchById(id);
    }
}
