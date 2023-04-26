package github.clone_code_detection.controller.highlight;

import github.clone_code_detection.entity.highlight.report.HighlightSessionDetailDTO;
import github.clone_code_detection.entity.highlight.report.HighlightSessionOverviewDTO;
import github.clone_code_detection.entity.highlight.report.HighlightSingleSourceDTO;
import github.clone_code_detection.entity.index.IndexInstruction;
import github.clone_code_detection.service.highlight.ServiceHighlight;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/highlight")
public class HighlightController {
    private final ServiceHighlight serviceHighlightTest;

    @Autowired
    public HighlightController(ServiceHighlight serviceHighlightTest) {
        this.serviceHighlightTest = serviceHighlightTest;
    }

    @PostMapping(path = "/query",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public HighlightSessionDetailDTO queryDocument(@RequestParam("source") MultipartFile source) {
        return serviceHighlightTest.highlight(source, IndexInstruction.getDefaultInstruction());
    }

    @GetMapping(path = "/get-session-by-id/{id}")
    @ResponseStatus(HttpStatus.OK)
    public HighlightSessionDetailDTO getSessionById(@PathVariable(name = "id") String id) {
        return serviceHighlightTest.getHighlightSessionById(id);
    }

    @GetMapping(path = "/all-session")
    @ResponseStatus(HttpStatus.OK)
    public HighlightSessionOverviewDTO getAllSession() {
        return HighlightSessionOverviewDTO.from(serviceHighlightTest.getAllSession());
    }

    @GetMapping(path = "/get-match-source/{id}")
    @ResponseStatus(HttpStatus.OK)
    public HighlightSingleSourceDTO getSingeSourceById(@PathVariable(name = "id") String id) {
        return serviceHighlightTest.getSingleSourceMatchById(id);
    }
}
