package github.clone_code_detection.entity.highlight.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import github.clone_code_detection.entity.highlight.document.ReportSourceDocument;
import github.clone_code_detection.entity.highlight.document.SimilarityReport;
import github.clone_code_detection.entity.highlight.document.SimilarityReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimilarityReportOverviewDTO {
    private Summary summary;
    private Collection<ReportDetail> collection;

    public static SimilarityReportOverviewDTO from(Collection<SimilarityReport.SimilarityReportDTO> sessions) {
        return SimilarityReportOverviewDTO.builder()
                                          .summary(Summary.builder().build())
                                          .collection(sessions.stream().map(ReportDetail::from).toList())
                                          .build();
    }

    @Builder
    @Data
    private static class Summary {
        private String language;
    }

    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class ReportDetail {
        private UUID id;
        private String name;
        private long created;
        private SimilarityReportStatus status;
        @JsonProperty("total_files")
        private int totalFiles;
        @JsonProperty("matched_files")
        private int matchedFiles;

        private static ReportDetail from(SimilarityReport.SimilarityReportDTO report) {
            Collection<ReportSourceDocument> sources = report.getSources();
            return ReportDetail.builder()
                               .id(report.getId())
                               .name(report.getName())
                               .created(report.getCreated().toEpochSecond())
                               .status(report.getStatus())
                               .totalFiles(sources.size())
                               .matchedFiles((int) sources
                                                         .stream()
                                                         .filter(match -> !match.getMatches().isEmpty())
                                                         .count())
                               .build();
        }
    }
}
