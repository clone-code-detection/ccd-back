package github.clone_code_detection.repo;

import github.clone_code_detection.entity.query.QueryInstruction;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class RepoElasticsearchQuery {
    public static final String SOURCE_CODE_FIELD = "source_code";
    private final RestHighLevelClient elasticsearchClient;

    @Autowired
    public RepoElasticsearchQuery(RestHighLevelClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    public SearchResponse query(QueryInstruction queryInstruction) throws
            IOException {
        SearchRequest searchRequest = this.buildSearchRequest(queryInstruction);
        log.info("[Repo es query] search request {}", searchRequest);
        return elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
    }

    protected static List<QueryBuilder> buildMustQuery(QueryInstruction queryInstruction) {
        String minimumShouldMatch = queryInstruction.getMinimumShouldMatch();
        return Collections.singletonList(QueryBuilders.matchQuery(SOURCE_CODE_FIELD, queryInstruction.getContent())
                                                      .minimumShouldMatch(minimumShouldMatch));
    }

    // TODO: Some logic here
    protected static List<QueryBuilder> buildFilterQuery(QueryInstruction queryInstruction) {
        List<QueryBuilder> queries = new ArrayList<>();
        for (Map.Entry<String, Object> entry : queryInstruction.getQueryMeta()
                                                               .entrySet()) {
            String s = entry.getKey();
            Object o = entry.getValue();
            if (!(o instanceof String)) continue;
            queries.add(QueryBuilders.termQuery(s, (String) o));
        }
        return queries;
    }


    private static HighlightBuilder buildHighlightQuery(QueryInstruction queryInstruction) {
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.order("score");
        //Build each type
        HighlightBuilder.Field highlightContent = new HighlightBuilder.Field(SOURCE_CODE_FIELD);
        highlightContent.fragmentSize(0);
        highlightContent.numOfFragments(0);
        highlightContent.highlighterType("experimental");
        //Add to builder
        highlightBuilder.field(highlightContent);

        return highlightBuilder;
    }

    private SearchRequest buildSearchRequest(QueryInstruction queryInstruction) {
        var indexes = queryInstruction.getLanguage();
        List<QueryBuilder> mustQuery = buildMustQuery(queryInstruction);
        List<QueryBuilder> filterQuery = buildFilterQuery(queryInstruction);


        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        mustQuery.forEach(boolQueryBuilder::must);
        filterQuery.forEach(boolQueryBuilder::filter);

        // build source
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder);
        if (queryInstruction.getIncludeHighlight()) {
            HighlightBuilder highlightBuilder = buildHighlightQuery(queryInstruction);
            searchSourceBuilder.highlighter(highlightBuilder);
        }

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.source(searchSourceBuilder)
                     .indices(indexes);

        return searchRequest;
    }
}
