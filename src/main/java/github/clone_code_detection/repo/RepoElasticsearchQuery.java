package github.clone_code_detection.repo;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;

@Slf4j
@Repository
public class RepoElasticsearchQuery {
    private final RestHighLevelClient elasticsearchClient;

    @Autowired
    public RepoElasticsearchQuery(RestHighLevelClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    public SearchResponse query(SearchRequest searchRequest) throws
            IOException {
        return elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
    }
}
