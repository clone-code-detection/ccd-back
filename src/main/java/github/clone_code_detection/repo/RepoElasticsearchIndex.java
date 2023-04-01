package github.clone_code_detection.repo;

import com.fasterxml.jackson.core.JsonProcessingException;
import github.clone_code_detection.entity.ElasticsearchDocument;
import github.clone_code_detection.exceptions.es.ElasticsearchRequestBuildException;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.stream.Stream;

@Slf4j
@Repository
public class RepoElasticsearchIndex {
    private final RestHighLevelClient elasticsearchClient;

    @Autowired
    public RepoElasticsearchIndex(RestHighLevelClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    private static IndexRequest apply(Pair<String, ElasticsearchDocument> langAndDoc) {
        String lang = langAndDoc.getFirst();
        ElasticsearchDocument doc = langAndDoc.getSecond();
        try {
            return new IndexRequest(lang).source(doc.asJson());
        } catch (JsonProcessingException e) {
            throw new ElasticsearchRequestBuildException("Failed to build index request");
        }
    }

    public BulkResponse indexDocuments(Stream<Pair<String, ElasticsearchDocument>> requests) throws IOException {
        BulkRequest request = new BulkRequest();
        requests.map(RepoElasticsearchIndex::apply)
                .forEach(request::add);
        return elasticsearchClient.bulk(request, RequestOptions.DEFAULT);
    }
}