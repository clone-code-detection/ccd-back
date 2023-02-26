package github.clone_code_detection.controllers;

import co.elastic.clients.elasticsearch.indices.analyze.AnalyzeToken;
import github.clone_code_detection.entity.ResponseUnified;
import github.clone_code_detection.repo.RepoElasticsearch;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.function.Function;

@RestController
@RequestMapping(("/api/cst"))
@Slf4j
class CSTProcessorRouter {
    final RepoElasticsearch repoElasticsearch;

    @Autowired
    public CSTProcessorRouter(RepoElasticsearch repoElasticsearch) {
        this.repoElasticsearch = repoElasticsearch;
    }

    @RequestMapping(value = "/analyze/java", method = RequestMethod.GET)
    ResponseEntity<ResponseUnified<Collection<String>>> analyzeJava(@RequestBody String body) {
        return getHandlingRequest(body , repoElasticsearch::analyzeJavaDocument);
    }

    @RequestMapping(value = "/analyze/python", method = RequestMethod.GET)
    ResponseEntity<ResponseUnified<Collection<String>>> analyzePython(@RequestBody String body) {
        return getHandlingRequest(body , repoElasticsearch::analyzePythonTokens);
    }

    @RequestMapping(value = "/analyze/csharp", method = RequestMethod.GET)
    ResponseEntity<ResponseUnified<Collection<String>>> analyzeCsharp(@RequestBody String body) {
        return getHandlingRequest(body , repoElasticsearch::analyzeCSharpTokens);
    }

    private ResponseEntity<ResponseUnified<Collection<String>>> getHandlingRequest(String document ,
                                                                                   Function<String, Collection<AnalyzeToken>> analyzer) {
        Collection<String> tokens = analyzer.apply(document)
                .stream()
                .map(AnalyzeToken::toString)
                .toList();
        var response = new ResponseUnified<>("success" , HttpServletResponse.SC_OK , tokens);
        return ResponseEntity.ok(response);
    }
}