package github.clone_code_detection.repo;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class RepoElasticsearchHighlight {
    private final RestHighLevelClient client;
    private final RepoFileDocument repoFileDocument;

    @Autowired
    public RepoElasticsearchHighlight(RestHighLevelClient client, RepoFileDocument repoFileDocument) {
        this.client = client;
        this.repoFileDocument = repoFileDocument;
    }
}
