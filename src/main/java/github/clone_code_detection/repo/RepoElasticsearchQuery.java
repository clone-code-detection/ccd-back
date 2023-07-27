package github.clone_code_detection.repo;

import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.entity.query.QueryInstruction;
import github.clone_code_detection.exceptions.elasticsearch.BadRequestException;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Slf4j
@Repository
public class RepoElasticsearchQuery {
    public static final String SOURCE_CODE_FIELD = "source_code";
    public static final String SOURCE_CODE_FIELD_NORMALIZED = "source_code_normalized";
    private final RestHighLevelClient elasticsearchClient;

    @Autowired
    public RepoElasticsearchQuery(RestHighLevelClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    @NonNull
    private static TermVectorsRequest buildTermVectorsRequest(FileDocument fileDocument) {
        String index = LanguageUtil.getInstance()
                                   .getIndexFromFileName(fileDocument.getFileName());
        String uuid = fileDocument.getId()
                                  .toString();
        TermVectorsRequest template = new TermVectorsRequest(index, uuid);
        template.setOffsets(true);
        template.setPositions(true);
        template.setFieldStatistics(false);
        template.setFields(SOURCE_CODE_FIELD);
        return template;
    }

    public static List<QueryBuilder> buildMustQuery(@Validated QueryInstruction queryInstruction) {
        String minimumShouldMatch = queryInstruction.getMinimumShouldMatch();
        String field;
        log.debug("Querying with config: min match {} and field {} and language {}",
                  minimumShouldMatch,
                  queryInstruction.getType(),
                  queryInstruction.getLanguage());
        if (1 == queryInstruction.getType()) field = SOURCE_CODE_FIELD;
        else field = SOURCE_CODE_FIELD_NORMALIZED;
        return Collections.singletonList(QueryBuilders
                                                 .matchQuery(field, queryInstruction.getContent())
                                                 .minimumShouldMatch(minimumShouldMatch));
    }

    protected static List<QueryBuilder> buildFilterQuery() {
        return new ArrayList<>();
    }

    public SearchResponse query(QueryInstruction queryInstruction) throws IOException {
        SearchRequest searchRequest = this.buildSearchRequest(queryInstruction);
        return elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
    }

    public MultiSearchResponse multiquery(Collection<QueryInstruction> instructions) throws IOException {
        MultiSearchRequest multiSearchRequest = buildMultisearchRequest(instructions);
        log.info("[Repo es query] Start multi search for {} documents", multiSearchRequest.requests()
                                                                                          .size());
        return multiQuery(multiSearchRequest);
    }

    public MultiSearchResponse multiQuery(MultiSearchRequest multiSearchRequest) throws IOException {
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
            return getMultiTermVectorsResponse(multiTermVectorsRequest);
        } catch (IOException ex) {
            log.error("Error fetching from elasticsearch", ex);
            throw new BadRequestException("Error retrieving term vectors from ES");
        }
    }

    public MultiTermVectorsResponse getMultiTermVectorsResponse(MultiTermVectorsRequest multiTermVectorsRequest) throws IOException {
        return elasticsearchClient.mtermvectors(multiTermVectorsRequest, RequestOptions.DEFAULT);
    }

    private SearchRequest buildSearchRequest(QueryInstruction queryInstruction) {
        var indexes = queryInstruction.getLanguage();
        List<QueryBuilder> mustQuery = buildMustQuery(queryInstruction);
        List<QueryBuilder> filterQuery = buildFilterQuery();


        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        mustQuery.forEach(boolQueryBuilder::must);
        filterQuery.forEach(boolQueryBuilder::filter);

        // build source
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder);

        SearchRequest searchRequest = new SearchRequest();
        log.debug("[Repo es] Search request: {}", searchRequest);
        searchRequest.source(searchSourceBuilder)
                     .indices(indexes);
        return searchRequest;
    }
}
