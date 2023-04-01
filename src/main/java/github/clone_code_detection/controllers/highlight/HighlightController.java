package github.clone_code_detection.controllers.highlight;

import github.clone_code_detection.service.highlight.ServiceHighlight2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/highlight")
public class HighlightController {
    private final ServiceHighlight2 serviceHighlightTest;

    @Autowired
    public HighlightController(ServiceHighlight2 serviceHighlightTest) {this.serviceHighlightTest = serviceHighlightTest;}

    @GetMapping("")
    public String test() {
        serviceHighlightTest.test(ServiceHighlight2.Test.builder()
                                                        .a("")
                                                        .b("210313")
                                                        .build());
        return "success";
    }

    @RequestMapping(path = "/query", method = RequestMethod.POST, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public void queryDocument(@RequestParam("file") MultipartFile file) {

    }
}
