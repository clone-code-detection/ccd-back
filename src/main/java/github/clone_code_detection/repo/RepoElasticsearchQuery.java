package github.clone_code_detection.repo;

import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.entity.query.QueryInstruction;
import github.clone_code_detection.exceptions.highlight.ElasticsearchQueryException;
import github.clone_code_detection.util.LanguageUtil;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.MultiTermVectorsRequest;
import org.elasticsearch.client.core.MultiTermVectorsResponse;
import org.elasticsearch.client.core.TermVectorsRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.*;

@Slf4j
@Repository
public class RepoElasticsearchQuery {
    public static final String SOURCE_CODE_FIELD = "source_code";
    private final RestHighLevelClient elasticsearchClient;

    @Autowired
    public RepoElasticsearchQuery(RestHighLevelClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    @NonNull
    private static TermVectorsRequest buildTermVectorsRequest(FileDocument fileDocument) {
        String index = LanguageUtil.getInstance().getIndexFromFileName(fileDocument.getFileName());
        String uuid = fileDocument.getId().toString();
        TermVectorsRequest template = new TermVectorsRequest(index, uuid);
        template.setOffsets(true);
        template.setPositions(true);
        template.setFieldStatistics(false);
        template.setFields(SOURCE_CODE_FIELD);
        return template;
    }

    protected static List<QueryBuilder> buildMustQuery(QueryInstruction queryInstruction) {
        String minimumShouldMatch = queryInstruction.getMinimumShouldMatch();
        return Collections.singletonList(QueryBuilders
                .matchQuery(SOURCE_CODE_FIELD, queryInstruction.getContent())
                .minimumShouldMatch(minimumShouldMatch));
    }

    protected static List<QueryBuilder> buildFilterQuery(QueryInstruction queryInstruction) {
        return new ArrayList<>();
    }

    private static HighlightBuilder buildHighlightQuery() {
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.order("score");
        //Build each type
        HighlightBuilder.Field highlightContent = new HighlightBuilder.Field(SOURCE_CODE_FIELD);
        highlightContent.fragmentSize(0);
        highlightContent.numOfFragments(0);
        highlightContent.highlighterType("experimental");
        highlightBuilder.options(Map.of("return_offsets", true));
        //Add to builder
        highlightBuilder.field(highlightContent);

        return highlightBuilder;
    }

    public SearchResponse query(QueryInstruction queryInstruction) throws IOException {
        SearchRequest searchRequest = this.buildSearchRequest(queryInstruction);
        return elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
    }

    public MultiSearchResponse multiquery(Collection<QueryInstruction> instructions) throws IOException {
        MultiSearchRequest multiSearchRequest = buildMultisearchRequest(instructions);
        log.info("[Repo es query] Start multi search for {} documents", multiSearchRequest.requests().size());
        return elasticsearchClient.msearch(multiSearchRequest, RequestOptions.DEFAULT);
    }

    private MultiSearchRequest buildMultisearchRequest(Collection<QueryInstruction> instructions) {
        MultiSearchRequest multiSearchRequest = new MultiSearchRequest();
        instructions.forEach(instruction -> multiSearchRequest.add(buildSearchRequest(instruction)));
        return multiSearchRequest;
    }

    public MultiTermVectorsResponse getMultiTermVectors(FileDocument... fileDocuments) {
        try {
            MultiTermVectorsRequest multiTermVectorsRequest = new MultiTermVectorsRequest();
            for (FileDocument fileDocument : fileDocuments) {
                TermVectorsRequest template = buildTermVectorsRequest(fileDocument);
                multiTermVectorsRequest.add(template);
            }
            return elasticsearchClient.mtermvectors(multiTermVectorsRequest, RequestOptions.DEFAULT);
        } catch (IOException ex) {
            log.error("Error fetching from elasticsearch", ex);
            throw new ElasticsearchQueryException("Error retrieving term vectors from ES");
        }
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
        if (Boolean.TRUE.equals(queryInstruction.getIncludeHighlight())) {
            HighlightBuilder highlightBuilder = buildHighlightQuery();
            searchSourceBuilder.highlighter(highlightBuilder);
        }

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.source(searchSourceBuilder).indices(indexes);
        return searchRequest;
    }
}
