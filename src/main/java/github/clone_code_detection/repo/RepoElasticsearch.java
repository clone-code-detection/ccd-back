package github.clone_code_detection.repo;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.AnalyzeRequest;
import co.elastic.clients.elasticsearch.indices.analyze.AnalyzeToken;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class RepoElasticsearch {
    private final ElasticsearchClient elasticsearchClient;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    public RepoElasticsearch(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    public Collection<AnalyzeToken> analyzeCSharpTokens(String document) {
        return getAnalyzeTokens(document , "csharp-analyzer");
    }

    public Collection<AnalyzeToken> analyzePythonTokens(String document) {
        return getAnalyzeTokens(document , "python-tokenizer");
    }

    public Collection<AnalyzeToken> analyzeJavaDocument(String document) {
        return getAnalyzeTokens(document , "java-tokenizer");
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
            logger.info("{}" , tokens);
            return tokens;
        } catch (IOException e) {
            logger.error("Error connecting to elasticsearch" , e);
            return Collections.emptyList();
        }
    }
}