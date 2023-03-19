package github.clone_code_detection.controllers;

import github.clone_code_detection.entity.*;
import github.clone_code_detection.service.ServiceHighlight;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/api/highlight")
public class HighlightController {
    final ServiceHighlight serviceHighlight;

    @Autowired
    public HighlightController(ServiceHighlight serviceHighlight) {
        this.serviceHighlight = serviceHighlight;
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(path = "/file", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseUnified<HighlightReport> highlightFile(@RequestBody HighlightDocument request) {
        // Validate document's languages, it muse be 1 at size
        if (request.getLanguages().size() != 1)
            return new ResponseUnified<>("To highlight a file / code fragment, only 1 language is accepted", HttpServletResponse.SC_BAD_REQUEST, null);
        Collection<ElasticsearchDocument> documents = serviceHighlight.highlight(request.toQuery());
        HighlightReport report = serviceHighlight.generateHighlightReport(documents, request);
        if (report == null)
            return new ResponseUnified<>("Can not create report", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
        return new ResponseUnified<>("success", HttpServletResponse.SC_OK, report);
    }
}
