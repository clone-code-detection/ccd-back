package github.clone_code_detection.controllers;

import co.elastic.clients.elasticsearch.core.BulkResponse;
import com.google.gson.Gson;
import github.clone_code_detection.entity.CrawlGitHubDocument;
import github.clone_code_detection.entity.ElasticsearchDocument;
import github.clone_code_detection.entity.ResponseUnified;
import github.clone_code_detection.service.ServiceGitHub;
import jakarta.servlet.http.HttpServletResponse;
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
    final static Gson gson = new Gson();
    private final ServiceGitHub serviceGitHub;

    public IndexController(ServiceGitHub serviceGitHub) {
        this.serviceGitHub = serviceGitHub;
    }

    @RequestMapping(path = "/zip", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE})
    public ResponseUnified<BulkResponse> indexGitHubRepository(String body, @RequestParam("file") MultipartFile file) {
        Collection<ElasticsearchDocument> contents = serviceGitHub.unzipAndGetContents(file);
        CrawlGitHubDocument crawlGitHubBody = gson.fromJson(body, CrawlGitHubDocument.class);
        BulkResponse br = serviceGitHub.buildRepositoryPayloads(contents, crawlGitHubBody);
        return new ResponseUnified<>("success", HttpServletResponse.SC_OK, br);
    }
}
