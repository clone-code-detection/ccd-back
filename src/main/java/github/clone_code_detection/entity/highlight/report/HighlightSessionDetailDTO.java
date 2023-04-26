package github.clone_code_detection.entity.highlight.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import github.clone_code_detection.entity.highlight.document.HighlightSessionDocument;
import github.clone_code_detection.entity.highlight.document.HighlightSingleDocument;
import github.clone_code_detection.util.LanguageUtil;
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
public class HighlightSessionDetailDTO {
    @JsonProperty
    private UUID id;

    @JsonProperty
    private Time created;

    @JsonProperty
    private String name;

    @JsonProperty("summary")
    private HighlightSessionSummary summary;

    @JsonProperty("matches")
    private Collection<HighlightSingleDocumentDTO> reports;

    public static HighlightSessionDetailDTO from(HighlightSessionDocument sessionDocument) {
        HighlightSessionDetailDTO sessionReportDTO = ModelMapperUtil.getMapper()
                .map(sessionDocument,
                        HighlightSessionDetailDTO.class);
        sessionReportDTO.reports = sessionDocument.getMatches()
                .stream()
                .map(HighlightSingleDocumentDTO::from)
                .toList();
        sessionReportDTO.summary = HighlightSessionSummary.from(sessionDocument.getMatches());
        return sessionReportDTO;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    private static class HighlightSingleDocumentDTO {
        private UUID id;
        @JsonProperty("file_name")
        private String fileName;
        @JsonProperty("total_matches")
        private Integer totalMatches;

        public static HighlightSingleDocumentDTO from(HighlightSingleDocument document) {
            return HighlightSingleDocumentDTO.builder()
                    .fileName(document.getSource()
                            .getFileName())
                    .id(document.getId())
                    .totalMatches(document.getMatches()
                            .size())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class HighlightSessionSummary {
        @JsonProperty("main_language")
        private String mainLanguage;
        @JsonProperty("total_files")
        private int totalFiles;
        @JsonProperty("total_match_files")
        private int totalMatchFiles;

        public static HighlightSessionSummary from(Collection<HighlightSingleDocument> matches) {
            HighlightSessionSummary summary = HighlightSessionSummary.builder().build();
            summary.setTotalFiles(matches.size());
            summary.setMainLanguage(LanguageUtil.getMainLanguageOfSingleDocuments(matches));
            // Get total unmatched files
            for (HighlightSingleDocument match : matches) {
                if (match.getMatches().size() > 0)
                    summary.totalMatchFiles++;
            }
            return summary;
        }
    }
}
