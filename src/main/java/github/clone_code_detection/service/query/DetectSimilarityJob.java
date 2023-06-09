package github.clone_code_detection.service.query;

import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.entity.highlight.document.ReportSourceDocument;
import github.clone_code_detection.entity.highlight.document.SimilarityReport;
import github.clone_code_detection.entity.highlight.document.SimilarityReportStatus;
import github.clone_code_detection.entity.index.IndexInstruction;
import github.clone_code_detection.entity.query.QueryInstruction;
import github.clone_code_detection.exceptions.highlight.ElasticsearchMultiHighlightException;
import github.clone_code_detection.exceptions.highlight.HighlightSessionException;
import github.clone_code_detection.repo.RepoElasticsearchQuery;
import github.clone_code_detection.repo.RepoSimilarityReport;
import github.clone_code_detection.service.index.ServiceIndex;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Slf4j
@Builder
@Data
public class DetectSimilarityJob implements Runnable {
    private RepoSimilarityReport repoSimilarityReport;
    private RepoElasticsearchQuery repoElasticsearchQuery;
    private ServiceQuery serviceQuery;
    private ServiceIndex serviceIndex;

    private UUID reportId;

    private IndexInstruction instruction;
    private QueryInstruction queryInstruction;
    private SimilarityReport report;
    private Integer batchSize;

    @Override
    @Transactional
    public void run() {
        try {
            markSessionAsProcessing();
            // Detect highlight session
            Collection<FileDocument> files = instruction.getFiles();
            List<ReportSourceDocument> hits = new ArrayList<>(multiHighlight(files));
            report.setSources(hits);
            report.setStatus(SimilarityReportStatus.DONE);
            repoSimilarityReport.save(report);
            serviceIndex.bulkIndexAllDocuments(instruction);
        } catch (Exception e) {
            log.error("[Service highlight] detect highlight session {} failed with error: {}",
                    report.getName(),
                    e.getMessage());
            // Update status to failed for future retry
            report.setStatus(SimilarityReportStatus.FAILED);
            report.setException(new HighlightSessionException("[Service highlight] Error while processing highlight",
                    e).toString());
            repoSimilarityReport.save(report);
        }
    }

    @Transactional
    public void markSessionAsProcessing() {
        report = repoSimilarityReport.findById(reportId)
                                     .orElseThrow();
        try {
            repoSimilarityReport.updateStatusByIdEquals(SimilarityReportStatus.PROCESSING, report.getId());
        } catch (Exception e) {
            log.error("[Service highlight] Can't update session to PROCESSING");
            report.setStatus(SimilarityReportStatus.FAILED);
            report.setException(new HighlightSessionException("[Service highlight] Can't update session to PROCESSING",
                    e).toString());
            repoSimilarityReport.save(report);
        }
    }

    public Collection<ReportSourceDocument> multiHighlight(Collection<FileDocument> files) {
        String minimumShouldMatch = queryInstruction.getMinimumShouldMatch();
        Collection<ReportSourceDocument> reportSourceDocuments = new ArrayList<>();
        int startIndex = 0;
        List<FileDocument> asList = files.stream()
                                         .toList();
        while (startIndex < files.size()) {
            int endIndex = Math.min(batchSize + startIndex, files.size());
            // Create multisearch query
            Collection<QueryInstruction> instructions = new ArrayList<>();
            for (int i = startIndex; i < endIndex; ++i) {
                QueryInstruction instruction = queryInstruction.clone();
                instruction.setQueryDocument(asList.get(i));
                instructions.add(instruction);
            }
            try {
                MultiSearchResponse multiSearchResponse = repoElasticsearchQuery.multiquery(instructions);
                for (int index = 0; index < multiSearchResponse.getResponses().length; ++index) {
                    MultiSearchResponse.Item searchResponse = multiSearchResponse.getResponses()[index];
                    if (searchResponse.isFailure()) {
                        log.error("[Service highlight] Search response in multi highlight is fail. Error: {}",
                                searchResponse.getFailureMessage());
                    } else if (searchResponse.getResponse() != null) {
                        reportSourceDocuments.add(serviceQuery.parseResponse(asList
                                                                                  .get(index + startIndex),
                                searchResponse.getResponse()));
                    }
                }
            } catch (IOException e) {
                log.error("[Service highlight] Multi highlight failed. Error: {}", e.getMessage());
                throw new ElasticsearchMultiHighlightException("Multi highlight failed", e);
            } finally {
                startIndex = endIndex;
            }
        }
        return reportSourceDocuments;
    }

}
