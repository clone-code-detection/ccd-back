package github.clone_code_detection.service.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import github.clone_code_detection.entity.authenication.UserImpl;
import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.entity.highlight.document.ReportSourceDocument;
import github.clone_code_detection.entity.highlight.document.ReportTargetDocument;
import github.clone_code_detection.entity.query.QueryInstruction;
import github.clone_code_detection.exceptions.highlight.ElasticsearchQueryException;
import github.clone_code_detection.repo.RepoElasticsearchQuery;
import github.clone_code_detection.repo.RepoFileDocument;
import github.clone_code_detection.repo.RepoReportSourceDocument;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class ServiceQuery {
    private final RepoElasticsearchQuery repoElasticsearchQuery;
    private final RepoReportSourceDocument repoReportSourceDocument;
    private final RepoFileDocument repoFileDocument;

    @Autowired
    public ServiceQuery(RepoElasticsearchQuery repoElasticsearchQuery,
                        RepoReportSourceDocument repoHighlightSessionDocument,
                        RepoFileDocument repoFileDocument) {
        this.repoElasticsearchQuery = repoElasticsearchQuery;
        this.repoReportSourceDocument = repoHighlightSessionDocument;
        this.repoFileDocument = repoFileDocument;
    }

    private static TargetMatchOverview extract(ReportTargetDocument document) {
        UserImpl user = document.getTarget().getUser();
        String userName = user != null ? user.getUsername() : "anonymous";
        String targetId = document.getId().toString();
        Float score = document.getScore();
        return TargetMatchOverview.builder().author(userName).id(targetId).score(score).build();
    }

    public Collection<TargetMatchOverview> handle(String id) {
        UUID fromString;
        try {
            fromString = UUID.fromString(id);
        } catch (Exception ig) {
            throw new RuntimeException("Invalid id", ig);
        }
        ReportSourceDocument singleDocument = repoReportSourceDocument.findById(fromString).orElseThrow();
        return singleDocument.getMatches().stream().map(ServiceQuery::extract).toList();
    }

    /**
     * For each file, query with highlight enabled
     */
    public ReportSourceDocument extractSingleDocument(FileDocument source) {
        QueryInstruction queryInstruction = QueryInstruction.builder()
                                                            .queryDocument(source)
                                                            .includeHighlight(true)
                                                            .minimumShouldMatch("40%")
                                                            .build();
        SearchResponse searchResponse;
        try {
            searchResponse = repoElasticsearchQuery.query(queryInstruction);
        } catch (IOException e) {
            log.error("Error querying elasticsearch", e);
            throw new ElasticsearchQueryException("[Service highlight] Failed to query es");
        }
        // parse response
        return parseResponse(source, searchResponse);
    }

    /**
     * Extract match fields from es search response
     */
    public ReportSourceDocument parseResponse(FileDocument source, SearchResponse search) {
        ReportSourceDocument.ReportSourceDocumentBuilder builder = ReportSourceDocument.builder();
        // get hits
        Collection<ReportTargetDocument> matches = new ArrayList<>();
        ReportSourceDocument reportSourceDocument = builder.source(source).matches(matches).build();
        for (SearchHit hit : search.getHits()) {
            String id = hit.getId();
            UUID fromString;
            try {
                fromString = UUID.fromString(id);
            } catch (Exception ig) {
                continue;
            }
            Optional<FileDocument> fileDocument = repoFileDocument.findById(fromString);
            if (fileDocument.isEmpty()) continue;
            ReportTargetDocument singleMatch = ReportTargetDocument.builder()
                                                                   .score(hit.getScore())
                                                                   .source(reportSourceDocument)
                                                                   .target(fileDocument.get())
                                                                   .build();
            matches.add(singleMatch);
        }
        return reportSourceDocument;
    }

    @Builder
    public static class TargetMatchOverview {
        @JsonProperty("author")
        private String author;

        @JsonProperty("score")
        private double score;

        @JsonProperty("target_match_id")
        private String id;
    }
}
