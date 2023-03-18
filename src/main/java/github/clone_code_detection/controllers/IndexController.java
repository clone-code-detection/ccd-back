package github.clone_code_detection.controllers;

import co.elastic.clients.elasticsearch.core.BulkResponse;
import github.clone_code_detection.entity.CrawlGitHubDocument;
import github.clone_code_detection.entity.ResponseUnified;
import github.clone_code_detection.service.ServiceGitHub;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;

@RestController
@RequestMapping("/api/index")
public class IndexController {
    private final ServiceGitHub serviceGitHub;

    public IndexController(ServiceGitHub serviceGitHub) {
        this.serviceGitHub = serviceGitHub;
    }

    @RequestMapping(path = "/zip", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE})
    public ResponseUnified<BulkResponse> indexGitHubRepository(CrawlGitHubDocument crawlGitHubBody,
                                                               @RequestParam("file") MultipartFile file) {
        Collection<String> contents = serviceGitHub.unzipAndGetContents(file);
        BulkResponse br = serviceGitHub.buildRepositoryPayloads(contents, crawlGitHubBody);
        return new ResponseUnified<>("success", HttpServletResponse.SC_OK, br);
    }
}
