package github.clone_code_detection.repo;

import com.fasterxml.jackson.core.JsonProcessingException;
import github.clone_code_detection.entity.ElasticsearchDocument;
import github.clone_code_detection.exceptions.elasticsearch.OperationException;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
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

    private static IndexRequest getIndexAndDoc(Pair<String, ElasticsearchDocument> langAndDoc) {
        String lang = langAndDoc.getFirst();
        ElasticsearchDocument doc = langAndDoc.getSecond();
        try {
            IndexRequest source = new IndexRequest(lang).source(doc.asJson(), XContentType.JSON);
            if (doc.getId() != null) source.id(doc.getId());
            return source;
        } catch (JsonProcessingException e) {
            throw new OperationException("Failed to build index request");
        }
    }

    public BulkResponse indexDocuments(Stream<Pair<String, ElasticsearchDocument>> requests) throws IOException {
        BulkRequest request = new BulkRequest().setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        requests.map(RepoElasticsearchIndex::getIndexAndDoc)
                .forEach(request::add);
        request.timeout(TimeValue.timeValueSeconds(120));
        return bulkIndex(request);
    }

    public BulkResponse bulkIndex(BulkRequest request) throws IOException {
        return elasticsearchClient.bulk(request, RequestOptions.DEFAULT);
    }

    public void createIndex(String indexName) throws IOException {
        CreateIndexRequest request = new CreateIndexRequest(indexName);
        CreateIndexResponse createIndexResponse = elasticsearchClient.indices().create(request, RequestOptions.DEFAULT);
        if (!createIndexResponse.isShardsAcknowledged() || !createIndexResponse.isAcknowledged())
            throw new RuntimeException();
    }
}