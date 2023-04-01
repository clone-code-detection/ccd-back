package github.clone_code_detection.service.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import github.clone_code_detection.entity.ElasticsearchDocument;
import github.clone_code_detection.entity.query.QueryDocument;
import github.clone_code_detection.exceptions.es.ElasticsearchResponseParseException;
import github.clone_code_detection.repo.RepoElasticsearchQuery;
import github.clone_code_detection.util.JsonUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ServiceQuery implements IServiceQuery {
    private final RepoElasticsearchQuery repoElasticsearchQuery;

    @Autowired
    public ServiceQuery(RepoElasticsearchQuery repoElasticsearchQuery) {
        this.repoElasticsearchQuery = repoElasticsearchQuery;
    }

    protected static List<QueryBuilder> buildMustQuery(QueryDocument queryDocument) {
        String minimumShouldMatch = queryDocument.getMinimumShouldMatch();
        return Collections.singletonList(QueryBuilders.matchQuery("source_code", queryDocument.getContent())
                                                      .minimumShouldMatch(minimumShouldMatch));
    }

    // TODO: Some logic here
    protected static List<QueryBuilder> buildFilterQuery(QueryDocument queryDocument) {
        List<QueryBuilder> queries = new ArrayList<>();
        for (Map.Entry<String, Object> entry : queryDocument.getQueryMeta()
                                                            .entrySet()) {
            String s = entry.getKey();
            Object o = entry.getValue();
            if (!(o instanceof String)) continue;
            queries.add(QueryBuilders.termQuery(s, (String) o));
        }
        return queries;
    }

    @SneakyThrows
    @Override
    public List<ElasticsearchDocument> search(@Nonnull QueryDocument queryDocument) {
        SearchRequest sr = buildSearchRequest(queryDocument);
        log.info("{}", sr.toString());
        return Arrays.stream(repoElasticsearchQuery.query(sr)
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

    private SearchRequest buildSearchRequest(QueryDocument queryDocument) {
        var indexes = queryDocument.getLanguage();
        List<QueryBuilder> mustQuery = buildMustQuery(queryDocument);
        List<QueryBuilder> filterQuery = buildFilterQuery(queryDocument);


        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        mustQuery.forEach(boolQueryBuilder::must);
        filterQuery.forEach(boolQueryBuilder::filter);

        // build source
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder);

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.source(searchSourceBuilder)
                     .indices(indexes);

        return searchRequest;
    }
}
