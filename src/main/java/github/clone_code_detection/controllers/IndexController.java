package github.clone_code_detection.controllers;

import co.elastic.clients.elasticsearch.core.BulkResponse;
import github.clone_code_detection.entity.CrawlGitHubDocument;
import github.clone_code_detection.service.ServiceGitHub;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;

@RestController
@RequestMapping("/api/index")
public class IndexController {
    private final ServiceGitHub serviceGitHub;

    public IndexController(ServiceGitHub serviceGitHub) {
        this.serviceGitHub = serviceGitHub;
    }

    @RequestMapping(path = "/zip", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE,
            MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE})
    public BulkResponse indexGitHubRepository(CrawlGitHubDocument crawlGitHubBody,
                                              @RequestParam("file") MultipartFile file) {
        Collection<String> contents = serviceGitHub.unzipAndGetContents(file);
        return serviceGitHub.buildRepositoryPayloads(contents, crawlGitHubBody);
    }
}
