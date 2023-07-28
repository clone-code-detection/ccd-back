package github.clone_code_detection.service.highlight;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.entity.highlight.document.ReportSourceDocument;
import github.clone_code_detection.entity.highlight.document.ReportTargetDocument;
import github.clone_code_detection.repo.RepoElasticsearchQuery;
import github.clone_code_detection.repo.RepoReportSourceDocument;
import github.clone_code_detection.repo.intra_project.RepoIntraProjectReport;
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
    private final RepoReportSourceDocument repoReportSourceDocument;
    private final RepoElasticsearchQuery repoElasticsearchQuery;
    private final ServiceHighlight serviceHighlight;
    private final RepoIntraProjectReport repoIntraProjectReport;

    @Autowired
    public ServiceAdvancedHighlight(RepoReportSourceDocument repoReportSourceDocument,
                                    RepoElasticsearchQuery repoElasticsearchQuery,
                                    ServiceHighlight serviceHighlight, RepoIntraProjectReport repoIntraProjectReport) {
        this.repoReportSourceDocument = repoReportSourceDocument;
        this.repoElasticsearchQuery = repoElasticsearchQuery;
        this.serviceHighlight = serviceHighlight;
        this.repoIntraProjectReport = repoIntraProjectReport;
    }

    public Collection<ExtendHighlightReturn> advanceHighlightBySourceId(String id) {
        if (id.equals("undefined")) return new ArrayList<>();
        ReportSourceDocument singleDocument = repoReportSourceDocument.findById(UUID.fromString(id)).orElseThrow();
        // Get intra project report
        String index = repoIntraProjectReport.getEsIndexBySourceId(singleDocument.getId());
        if (index == null) return new ArrayList<>();
        List<FileDocument> documents = new ArrayList<>(singleDocument.getMatches()
                                                                     .stream()
                                                                     .map(ReportTargetDocument::getTarget)
                                                                     .toList());
        documents.add(singleDocument.getSource());
        MultiTermVectorsResponse multiTermVectors = repoElasticsearchQuery.getMultiTermVectors(index, documents.toArray(
                FileDocument[]::new));

        ReportTargetDocument[] matches = singleDocument.getMatches().toArray(ReportTargetDocument[]::new);

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
        @JsonUnwrapped
        private final ServiceHighlight.HighlightReturn highlightReturn;
        @JsonProperty("highlight_target_id")
        private String targetId;

        public ExtendHighlightReturn(ServiceHighlight.HighlightReturn highlightReturn) {
            this.highlightReturn = highlightReturn;
        }

        public ExtendHighlightReturn targetId(String targetId) {
            this.targetId = targetId;
            return this;
        }
    }
}
