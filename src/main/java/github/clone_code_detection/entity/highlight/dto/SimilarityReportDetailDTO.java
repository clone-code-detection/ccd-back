package github.clone_code_detection.entity.highlight.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import github.clone_code_detection.entity.highlight.document.ReportSourceDocument;
import github.clone_code_detection.entity.highlight.document.SimilarityReport;
import github.clone_code_detection.util.ModelMapperUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Time;
import java.util.Collection;
import java.util.UUID;

@Builder
@Data

@NoArgsConstructor
@AllArgsConstructor
public class SimilarityReportDetailDTO {
    @JsonProperty
    private UUID id;

    @JsonProperty
    private Time created;

    @JsonProperty
    private String name;

    @JsonProperty("summary")
    private Summary summary;

    @JsonProperty("matches")
    private Collection<Document> reports;

    public static SimilarityReportDetailDTO from(SimilarityReport sessionDocument) {
        SimilarityReportDetailDTO sessionReportDTO = ModelMapperUtil.getMapper()
                                                                    .map(sessionDocument,
                                                                         SimilarityReportDetailDTO.class);
        sessionReportDTO.reports = sessionDocument.getSources().stream().map(Document::from).toList();
        sessionReportDTO.summary = Summary.from(sessionDocument);
        return sessionReportDTO;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    private static class Document {
        private UUID id;
        @JsonProperty("file_name")
        private String fileName;
        @JsonProperty("total_matches")
        private Integer totalMatches;

        public static Document from(ReportSourceDocument document) {
            return Document.builder()
                           .fileName(document.getSource().getFileName())
                           .id(document.getId())
                           .totalMatches(document.getMatches().size())
                           .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Summary {
        @JsonProperty("total_files")
        private int totalFiles;
        @JsonProperty("total_match_files")
        private int totalMatchFiles;

        public static Summary from(SimilarityReport document) {
            Summary summary = Summary.builder().build();
            summary.setTotalFiles(document.getSources().size());
            // Get total unmatched files
            for (ReportSourceDocument match : document.getSources()) {
                if (!match.getMatches().isEmpty()) summary.totalMatchFiles++;
            }
            return summary;
        }
    }
}