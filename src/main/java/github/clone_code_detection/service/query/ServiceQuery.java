package github.clone_code_detection.service.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import github.clone_code_detection.entity.ElasticsearchDocument;
import github.clone_code_detection.entity.authenication.UserImpl;
import github.clone_code_detection.entity.highlight.document.HighlightSingleDocument;
import github.clone_code_detection.entity.highlight.document.HighlightSingleTargetMatchDocument;
import github.clone_code_detection.entity.query.QueryInstruction;
import github.clone_code_detection.exceptions.es.ElasticsearchResponseParseException;
import github.clone_code_detection.repo.RepoElasticsearchQuery;
import github.clone_code_detection.repo.RepoHighlightSingleMatchDocument;
import github.clone_code_detection.util.JsonUtil;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ServiceQuery implements IServiceQuery {
    private final RepoElasticsearchQuery repoElasticsearchQuery;
    private final RepoHighlightSingleMatchDocument repoHighlightSingleMatchDocument;

    @Autowired
    public ServiceQuery(RepoElasticsearchQuery repoElasticsearchQuery, RepoHighlightSingleMatchDocument repoHighlightSessionDocument) {
        this.repoElasticsearchQuery = repoElasticsearchQuery;
        this.repoHighlightSingleMatchDocument = repoHighlightSessionDocument;
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

    public Collection<TargetMatchOverview> handle(String id) {
        UUID fromString;
        try {
            fromString = UUID.fromString(id);
        } catch (Exception ig) {
            throw new RuntimeException("Invalid id", ig);
        }
        HighlightSingleDocument singleDocument = repoHighlightSingleMatchDocument.findById(fromString)
                                                                                 .orElseThrow();
        return singleDocument.getMatches()
                             .stream()
                             .map(ServiceQuery::extract)
                             .toList();
    }

    private static TargetMatchOverview extract(HighlightSingleTargetMatchDocument document) {
        UserImpl user = document.getTarget()
                                .getUser();
        String userName = user != null ? user.getUsername() : "anonymous";
        String targetId = document.getId()
                                  .toString();
        Float score = document.getScore();
        return TargetMatchOverview.builder()
                                  .author(userName)
                                  .id(targetId)
                                  .score(score)
                                  .build();
    }

    @Builder
    public static class TargetMatchOverview {
        @JsonProperty("author")
        private String author;

        @JsonProperty("score")
        private double score;

        @JsonProperty("target_match_id")
        private String id;

        // TODO: created time, origin
    }
}
