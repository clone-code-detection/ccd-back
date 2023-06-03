package github.clone_code_detection.service.highlight;

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
import github.clone_code_detection.service.query.ServiceQuery;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Slf4j
class DetectSimilarityJob implements Runnable {
    private final UUID reportId;
    private final IndexInstruction instruction;
    private final ServiceIndex serviceIndex;
    private final RepoSimilarityReport repoSimilarityReport;
    private final RepoElasticsearchQuery repoElasticsearchQuery;
    private final ServiceQuery serviceQuery;
    private final int batchSize;
    private final String minimumShouldMatch;
    private SimilarityReport report;

    public DetectSimilarityJob(UUID reportId,
                               IndexInstruction instruction,
                               ServiceIndex serviceIndex,
                               RepoSimilarityReport repoSimilarityReport,
                               RepoElasticsearchQuery repoElasticsearchQuery,
                               ServiceQuery serviceQuery,
                               int batchSize,
                               String minimumShouldMatch) {
        this.serviceIndex = serviceIndex;
        this.repoSimilarityReport = repoSimilarityReport;
        this.reportId = reportId;
        this.instruction = instruction;
        this.repoElasticsearchQuery = repoElasticsearchQuery;
        this.serviceQuery = serviceQuery;
        this.batchSize = batchSize;
        this.minimumShouldMatch = minimumShouldMatch;
        this.report = null;
    }

    @Override
    @Transactional
    public void run() {
        try {
            markSessionAsProcessing();
            // Detect highlight session
            Collection<FileDocument> files = instruction.getFiles();
            List<ReportSourceDocument> hits = new ArrayList<>(multihighlight(files));
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
        report = repoSimilarityReport.findById(reportId).orElseThrow();
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

    public Collection<ReportSourceDocument> multihighlight(Collection<FileDocument> files) {
        Collection<ReportSourceDocument> reportSourceDocuments = new ArrayList<>();
        int startIndex = 0;
        while (startIndex < files.size()) {
            int endIndex = Math.min(batchSize + startIndex, files.size());
            // Create multisearch query
            Collection<QueryInstruction> instructions = new ArrayList<>();
            for (int i = startIndex; i < endIndex; ++i)
                instructions.add(QueryInstruction.builder()
                                                 .queryDocument(files.stream().toList().get(i))
                                                 .includeHighlight(true)
                                                 .minimumShouldMatch(minimumShouldMatch)
                                                 .build());
            try {
                MultiSearchResponse multiSearchResponse = repoElasticsearchQuery.multiquery(instructions);
                for (int index = 0; index < multiSearchResponse.getResponses().length; ++index) {
                    MultiSearchResponse.Item searchResponse = multiSearchResponse.getResponses()[index];
                    if (searchResponse.isFailure()) {
                        log.error("[Service highlight] Search response in multi highlight is fail. Error: {}",
                                  searchResponse.getFailureMessage());
                    } else if (searchResponse.getResponse() != null) {
                        reportSourceDocuments.add(serviceQuery.parseResponse(files.stream()
                                                                                  .toList()
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
