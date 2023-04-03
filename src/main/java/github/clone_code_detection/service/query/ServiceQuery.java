package github.clone_code_detection.service.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import github.clone_code_detection.entity.ElasticsearchDocument;
import github.clone_code_detection.entity.query.QueryInstruction;
import github.clone_code_detection.exceptions.es.ElasticsearchResponseParseException;
import github.clone_code_detection.repo.RepoElasticsearchQuery;
import github.clone_code_detection.util.JsonUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ServiceQuery implements IServiceQuery {
    private final RepoElasticsearchQuery repoElasticsearchQuery;

    @Autowired
    public ServiceQuery(RepoElasticsearchQuery repoElasticsearchQuery) {
        this.repoElasticsearchQuery = repoElasticsearchQuery;
    }

    @SneakyThrows
    @Override
    public List<ElasticsearchDocument> search(@Nonnull QueryInstruction queryInstruction) {
        return Arrays.stream(repoElasticsearchQuery.query(queryInstruction)
                                                   .getHits()
                                                   .getHits())
                     .map(SearchHit::getSourceAsString)
                     .map(ServiceQuery::parseResponse)
                     .collect(Collectors.toList());
    }

    private static ElasticsearchDocument parseResponse(String value) {
        try {
            return JsonUtil.jsonFromString(value, ElasticsearchDocument.class);
        } catch (JsonProcessingException e) {
            throw new ElasticsearchResponseParseException("Failed to parse elasticsearch response", e);
        }
    }
}
