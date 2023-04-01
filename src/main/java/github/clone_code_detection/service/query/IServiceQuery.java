package github.clone_code_detection.service.query;

import github.clone_code_detection.entity.ElasticsearchDocument;
import github.clone_code_detection.entity.query.QueryDocument;

import java.util.List;

public interface IServiceQuery {
    List<ElasticsearchDocument> search(QueryDocument queryDocument);
}
