package github.clone_code_detection.repo;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.MsearchRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.util.ObjectBuilder;
import github.clone_code_detection.entity.ElasticsearchDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.function.Function;

@Slf4j
@Repository
public class RepoElasticsearchQuery {
    private final ElasticsearchClient elasticsearchClient;

    @Autowired
    public RepoElasticsearchQuery(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    public SearchResponse<ElasticsearchDocument> query(SearchRequest searchRequest) throws
            IOException {
        return elasticsearchClient.search(searchRequest , ElasticsearchDocument.class);
    }
}
