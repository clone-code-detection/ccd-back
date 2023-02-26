package github.clone_code_detection.repo;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.AnalyzeRequest;
import co.elastic.clients.elasticsearch.indices.analyze.AnalyzeToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Slf4j
@Repository
public class RepoElasticsearch {
    private final ElasticsearchClient elasticsearchClient;

    @Autowired
    public RepoElasticsearch(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    public Collection<AnalyzeToken> analyzeCSharpTokens(String document) {
        return getAnalyzeTokens(document , "csharp");
    }

    public Collection<AnalyzeToken> analyzePythonTokens(String document) {
        return getAnalyzeTokens(document , "python");
    }

    public Collection<AnalyzeToken> analyzeJavaDocument(String document) {
        return getAnalyzeTokens(document , "java");
    }

    private List<AnalyzeToken> getAnalyzeTokens(String document , String tokenizerName) {
        var analyzeRequest = new AnalyzeRequest.Builder().tokenizer(
                        tokenizerBuilder -> tokenizerBuilder.name(tokenizerName))
                .text(document)
                .build();
        try {
            List<AnalyzeToken> tokens = elasticsearchClient.indices()
                    .analyze(analyzeRequest)
                    .tokens();
            log.info("{}" , tokens);
            return tokens;
        } catch (IOException e) {
            log.error("Error connecting to elasticsearch" , e);
            return Collections.emptyList();
        }
    }
}