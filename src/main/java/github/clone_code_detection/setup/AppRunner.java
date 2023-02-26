package github.clone_code_detection.setup;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import github.clone_code_detection.entity.ElasticsearchDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class AppRunner implements ApplicationRunner {
    private final ElasticsearchClient elasticsearchClient;

    @Autowired
    public AppRunner(ElasticsearchClient elasticsearchClient) {

        this.elasticsearchClient = elasticsearchClient;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        ElasticsearchDocument esDocument = ElasticsearchDocument.builder()
                .meta(Map.of("user" , "hanh4"))
                .sourceCode("print(40020301)")
                .build();
        IndexResponse jsonValue = elasticsearchClient.index(i -> i.index("python")
                .document(esDocument));
        log.info("Response: {}" , jsonValue);
    }
}
