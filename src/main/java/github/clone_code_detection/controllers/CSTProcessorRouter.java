package github.clone_code_detection.controllers;

import co.elastic.clients.elasticsearch.indices.analyze.AnalyzeToken;
import github.clone_code_detection.repo.RepoElasticsearchAnalyze;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.function.Function;

@RestController
@RequestMapping(("/api/cst"))
@Slf4j
class CSTProcessorRouter {
    final RepoElasticsearchAnalyze repoElasticsearch;

    @Autowired
    public CSTProcessorRouter(RepoElasticsearchAnalyze repoElasticsearch) {
        this.repoElasticsearch = repoElasticsearch;
    }

    @RequestMapping(value = "/analyze/java", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    Collection<String> analyzeJava(@RequestBody String body) {
        return getHandlingRequest(body, repoElasticsearch::analyzeJavaDocument);
    }

    @RequestMapping(value = "/analyze/python", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    Collection<String> analyzePython(@RequestBody String body) {
        return getHandlingRequest(body, repoElasticsearch::analyzePythonTokens);
    }

    @RequestMapping(value = "/analyze/csharp", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    Collection<String> analyzeCsharp(@RequestBody String body) {
        return getHandlingRequest(body, repoElasticsearch::analyzeCSharpTokens);
    }

    private Collection<String> getHandlingRequest(String document,
                                                                  Function<String, Collection<AnalyzeToken>> analyzer) {
        return analyzer.apply(document)
                       .stream()
                       .map(AnalyzeToken::toString)
                       .toList();
    }
}