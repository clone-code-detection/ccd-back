package github.clone_code_detection.controller.highlight;

import github.clone_code_detection.service.highlight.ServiceHighlight2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/highlight")
public class HighlightController {
    private final ServiceHighlight2 serviceHighlightTest;

    @Autowired
    public HighlightController(ServiceHighlight2 serviceHighlightTest) {this.serviceHighlightTest = serviceHighlightTest;}

    @RequestMapping(path = "/query", method = RequestMethod.POST, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public void queryDocument(@RequestParam("file") MultipartFile file) {
        serviceHighlightTest.test(file, null);
    }
}
