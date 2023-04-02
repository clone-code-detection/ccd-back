package github.clone_code_detection.repo;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Slf4j
@Repository
public class RepoHighlight {
    private final RestHighLevelClient client;

    @Autowired
    public RepoHighlight(RestHighLevelClient client) {this.client = client;}

}
