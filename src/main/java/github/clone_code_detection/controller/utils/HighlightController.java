//package github.clone_code_detection.controllers;
//
//import github.clone_code_detection.entity.*;
//import github.clone_code_detection.entity.highlight.HighlightDocument;
//import github.clone_code_detection.entity.highlight.HighlightReport;
//
//import github.clone_code_detection.exceptions.es.highlight.HighlightLanguageException;
//import github.clone_code_detection.service.highlight.ServiceHighlight;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.MediaType;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Collection;
//
//@RestController
//@RequestMapping("/api/highlight")
//public class HighlightController {
//    final ServiceHighlight serviceHighlight;
//
//    @Autowired
//    public HighlightController(ServiceHighlight serviceHighlight) {
//        this.serviceHighlight = serviceHighlight;
//    }
//
//    @CrossOrigin(origins = "*")
//    @RequestMapping(path = "/file", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE})
//    public HighlightReport highlightFile(@RequestBody HighlightDocument request) {
//        // Validate document's languages, it muse be 1 at size
//        if (request.getLanguages()
//                   .size() != 1)
//            throw new HighlightLanguageException("To highlight a file / code fragment, only 1 language is accepted");
//        Collection<ElasticsearchDocument> documents = serviceHighlight.highlight(request.toQuery());
//        HighlightReport report = serviceHighlight.generateHighlightReport(documents, request);
//        assert report != null : "Internal server error";
//        return report;
//    }
//}
