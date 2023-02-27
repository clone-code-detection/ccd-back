package github.clone_code_detection.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import github.clone_code_detection.entity.QueryDocument;

import java.util.List;

public interface IServiceQuery {
    List<ElasticsearchClient> search(QueryDocument queryDocument);
}
