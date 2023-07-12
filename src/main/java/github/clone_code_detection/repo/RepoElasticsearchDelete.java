package github.clone_code_detection.repo;

import github.clone_code_detection.exceptions.elasticsearch.OperationException;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.modelmapper.internal.Pair;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;

@Slf4j
@Repository
public class RepoElasticsearchDelete {
    private final RestHighLevelClient elasticsearchClient;

    public RepoElasticsearchDelete(RestHighLevelClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    public void bulkDeleteByIds(List<Pair<String, String>> pairIndexWithId) throws IOException {
        BulkRequest request = new BulkRequest();
        pairIndexWithId.forEach(pair -> request.add(new DeleteRequest(pair.getLeft(), pair.getRight())));
        BulkResponse response = elasticsearchClient.bulk(request, RequestOptions.DEFAULT);
        response.forEach(item -> {
            if (item.isFailed()) {
                log.error("[repo es delete] Document {} delete failed. Error: {}",
                          item.getId(),
                          item.getFailureMessage());
                throw new OperationException("Failed to delete out date documents");
            }
        });
    }
}
