package github.clone_code_detection.controllers;

import github.clone_code_detection.entity.CrawlGitHubDocument;
import github.clone_code_detection.entity.IndexDocument;
import github.clone_code_detection.entity.ResponseUnified;
import github.clone_code_detection.service.ServiceGitHub;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;

@RestController
@RequestMapping("/api/github-crawl")
public class GitHubCrawlController {
    @RequestMapping(path = "/index", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE})
    public ResponseEntity<ResponseUnified<String>> indexGitHubRepository(@RequestPart("body") CrawlGitHubDocument crawlGitHubBody, @RequestPart("file") MultipartFile file) {
        Collection<String> contents = ServiceGitHub.unzipAndGetContents(file);
        Collection<IndexDocument> payloads = ServiceGitHub.buildRepositoryPayloads(contents, crawlGitHubBody);
        return ResponseEntity.ok(new ResponseUnified<>("success", HttpServletResponse.SC_OK, "Number of files received: " + payloads.size()));
    }

}
