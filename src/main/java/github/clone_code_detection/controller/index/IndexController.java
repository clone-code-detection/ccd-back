package github.clone_code_detection.controller.index;

import github.clone_code_detection.entity.index.IndexInstruction;
import github.clone_code_detection.service.index.ServiceIndex;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/index")
public class IndexController {
    private final ServiceIndex serviceIndex;

    public IndexController(ServiceIndex serviceIndex) {
        this.serviceIndex = serviceIndex;
    }

    @RequestMapping(path = "/zip", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE})
    public void indexGitHubRepository(IndexInstruction crawlGitHubBody, @RequestParam("file") MultipartFile file) {
        serviceIndex.indexAllDocuments(file, crawlGitHubBody);
    }
}
