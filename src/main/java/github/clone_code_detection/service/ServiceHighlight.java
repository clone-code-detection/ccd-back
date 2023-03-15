package github.clone_code_detection.service;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import github.clone_code_detection.entity.HighlightDocument;
import github.clone_code_detection.entity.QueryDocument;
import github.clone_code_detection.repo.RepoElasticsearchQuery;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ServiceHighlight extends ServiceQuery {
    final RepoElasticsearchQuery repoElasticsearchQuery;

    @Autowired
    public ServiceHighlight(RepoElasticsearchQuery repoElasticsearchQuery) {
        super(repoElasticsearchQuery);
        this.repoElasticsearchQuery = repoElasticsearchQuery;
    }

    @SneakyThrows
    public Collection<HighlightDocument> highlight(QueryDocument queryDocument) {
        // Call elasticsearch to highlight
        // Build highlight request
        SearchRequest searchRequest = buildHighlightSearchRequest(queryDocument);
        // Log
        log.info("{}", searchRequest);

        // Query with highlight option
        return repoElasticsearchQuery.query(searchRequest)
                .hits()
                .hits()
                .stream()
                .map(hit -> HighlightDocument.builder()
                        .source(hit.source())
                        .highlights(hit.highlight().get("source_code"))
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private SearchRequest buildHighlightSearchRequest(QueryDocument queryDocument) {
        // Get list of index table
        List<String> indexes = queryDocument.getLanguages().stream().toList();
        // build highlight request
        List<Query> mustQuery = buildMustQuery(queryDocument);
        List<Query> filterQuery = buildFilterQuery(queryDocument);
        Highlight highlight = buildHighlightFields();
        var query = BoolQuery.of(bp -> bp.filter(filterQuery).must(mustQuery))._toQuery();
        return SearchRequest.of(sr -> sr.index(indexes).query(query).highlight(highlight));
    }

    private Highlight buildHighlightFields() {
        return Highlight.of(highlight -> highlight.fields("source_code", new HighlightField.Builder().preTags("").postTags("").build()));
    }
}
