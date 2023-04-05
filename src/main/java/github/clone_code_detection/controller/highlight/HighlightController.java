package github.clone_code_detection.controller.highlight;

import github.clone_code_detection.entity.highlight.document.HighlightSessionDocument;
import github.clone_code_detection.entity.highlight.report.HighlightSessionReportDTO;
import github.clone_code_detection.entity.highlight.report.HighlightSingleMatchDTO;
import github.clone_code_detection.entity.index.IndexInstruction;
import github.clone_code_detection.service.highlight.ServiceHighlight2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;


@RestController
@RequestMapping("/api/highlight")
public class HighlightController {
    private final ServiceHighlight2 serviceHighlightTest;

    @Autowired
    public HighlightController(ServiceHighlight2 serviceHighlightTest) {this.serviceHighlightTest = serviceHighlightTest;}

    @RequestMapping(path = "/query", method = RequestMethod.POST, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public HighlightSessionReportDTO queryDocument(@RequestParam("source") MultipartFile source) {

        return serviceHighlightTest.highlight(source, IndexInstruction.getDefaultInstruction());
    }

    @RequestMapping(path = "/all-session", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Collection<HighlightSessionDocument.HighlightSessionProjection> getAllSession() {
        return serviceHighlightTest.getAllSession();
    }

    @RequestMapping(path = "/get-match/{id}", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public HighlightSingleMatchDTO getMatchById(@PathVariable(name = "id") String id) {
        return serviceHighlightTest.getSingleMatchBySessionId(id);
    }
}
