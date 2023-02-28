package github.clone_code_detection.repo;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import github.clone_code_detection.entity.ElasticsearchDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Repository
public class RepoElasticsearchIndex {
    private final ElasticsearchClient elasticsearchClient;

    @Autowired
    public RepoElasticsearchIndex(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    private static IndexOperation<ElasticsearchDocument> buildIndexOperation(
            IndexRequest<ElasticsearchDocument> indexRequest) {
        return new IndexOperation.Builder<ElasticsearchDocument>()
                .index(indexRequest.index())
                .document(indexRequest.document())
                .build();
    }

    public BulkResponse indexDocuments(Stream<IndexRequest<ElasticsearchDocument>> requests) throws
            IOException {
        BulkRequest.Builder br = new BulkRequest.Builder();
        var builderStream = requests.map(RepoElasticsearchIndex::buildIndexOperation)
                .map(BulkOperation::new)
                .collect(Collectors.toList());
        br.operations(builderStream);
        return elasticsearchClient.bulk(br.build());
    }
}