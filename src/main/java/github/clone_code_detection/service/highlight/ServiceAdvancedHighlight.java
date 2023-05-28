package github.clone_code_detection.service.highlight;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.entity.highlight.document.HighlightSingleDocument;
import github.clone_code_detection.entity.highlight.document.HighlightSingleTargetMatchDocument;
import github.clone_code_detection.repo.RepoElasticsearchQuery;
import github.clone_code_detection.repo.RepoHighlightSingleMatchDocument;
import org.elasticsearch.client.core.MultiTermVectorsResponse;
import org.elasticsearch.client.core.TermVectorsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
public class ServiceAdvancedHighlight {
    private final RepoHighlightSingleMatchDocument repoHighlightSingleMatchDocument;
    private final RepoElasticsearchQuery repoElasticsearchQuery;
    private final ServiceHighlight serviceHighlight;

    @Autowired
    public ServiceAdvancedHighlight(RepoHighlightSingleMatchDocument repoHighlightSingleMatchDocument, RepoElasticsearchQuery repoElasticsearchQuery, ServiceHighlight serviceHighlight) {
        this.repoHighlightSingleMatchDocument = repoHighlightSingleMatchDocument;
        this.repoElasticsearchQuery = repoElasticsearchQuery;
        this.serviceHighlight = serviceHighlight;
    }

    public Collection<ExtendHighlightReturn> advanceHighlightBySourceId(String id) {
        if (id.equals("undefined")) return new ArrayList<>();
        HighlightSingleDocument singleDocument = repoHighlightSingleMatchDocument.findById(UUID.fromString(id))
                                                                                 .orElseThrow();
        List<FileDocument> documents = new ArrayList<>(singleDocument.getMatches()
                                                                     .stream()
                                                                     .map(HighlightSingleTargetMatchDocument::getTarget)
                                                                     .toList());
        documents.add(singleDocument.getSource());
        MultiTermVectorsResponse multiTermVectors = repoElasticsearchQuery.getMultiTermVectors(
                documents.toArray(FileDocument[]::new));

        HighlightSingleTargetMatchDocument[] matches = singleDocument.getMatches()
                                                                     .toArray(
                                                                             HighlightSingleTargetMatchDocument[]::new);

        var res = new ArrayList<ExtendHighlightReturn>();
        List<TermVectorsResponse> termVectorsResponses = multiTermVectors.getTermVectorsResponses();
        var source = termVectorsResponses.get(termVectorsResponses.size() - 1);
        for (int i = 0; i < matches.length; i++) {
            var targetId = matches[i].getId().toString();
            var target = termVectorsResponses.get(i);
            List<ExtendHighlightReturn> collected = serviceHighlight.getExtractReturnCollection(source, target)
                                                                    .stream()
                                                                    .map(ExtendHighlightReturn::new)
                                                                    .map(extendHighlightReturn -> extendHighlightReturn.targetId(
                                                                            targetId))
                                                                    .toList();
            res.addAll(collected);
        }
        return res;
    }

    public static class ExtendHighlightReturn {
        @JsonProperty("highlight_target_id")
        private String targetId;

        @JsonUnwrapped
        private final ServiceHighlight.HighlightReturn highlightReturn;

        public ExtendHighlightReturn(ServiceHighlight.HighlightReturn highlightReturn) {
            this.highlightReturn = highlightReturn;
        }

        public ExtendHighlightReturn targetId(String targetId) {
            this.targetId = targetId;
            return this;
        }
    }
}
