package github.clone_code_detection.service;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import github.clone_code_detection.entity.ElasticsearchDocument;
import github.clone_code_detection.entity.QueryDocument;
import github.clone_code_detection.repo.RepoElasticsearchQuery;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    protected static List<Query> buildMustQuery(QueryDocument queryDocument) {
        String minimumShouldMatch = queryDocument.getMinimumShouldMatch();
        var matchQuery = MatchQuery.of(mq -> mq.field("source_code")
                        .query(queryDocument.getContent())
                        .minimumShouldMatch(minimumShouldMatch))
                ._toQuery();
        return Collections.singletonList(matchQuery);
    }

    // TODO: Some logic here
    protected static List<Query> buildFilterQuery(QueryDocument queryDocument) {
        List<Query> queries = new ArrayList<>();
        for (Map.Entry<String, Object> entry : queryDocument.getQueryMeta()
                .entrySet()) {
            String s = entry.getKey();
            Object o = entry.getValue();
            if (!(o instanceof String)) continue;

            var filterQuery = TermQuery.of(tq -> tq.field(s)
                            .value((String) o))
                    ._toQuery();
            queries.add(filterQuery);
        }
        return queries;
    }

    @SneakyThrows
    @Override
    public List<ElasticsearchDocument> search(QueryDocument queryDocument) {
        SearchRequest sr = buildSearchRequest(queryDocument);
        log.info("{}", sr.toString());
        return repoElasticsearchQuery.query(sr)
                .hits()
                .hits()
                .stream()
                .map(Hit::source)
                .collect(Collectors.toList());
    }

    private List<String> mapIndexes(Collection<String> indexes) {
        return indexes.stream()
                .toList();
    }

    private SearchRequest buildSearchRequest(QueryDocument queryDocument) {
//        List<String> indexes = mapIndexes(queryDocument.getLanguages());
        List<Query> mustQuery = buildMustQuery(queryDocument);
        List<Query> filterQuery = buildFilterQuery(queryDocument);
        var boolQuery = BoolQuery.of(bq -> bq.filter(filterQuery)
                        .must(mustQuery))
                ._toQuery();
        return SearchRequest.of(sr -> sr.index(List.of("java_method")).size(queryDocument.getSize())
                .query(boolQuery));
    }
}
