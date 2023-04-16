package github.clone_code_detection.entity.highlight.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import github.clone_code_detection.entity.highlight.document.HighlightSessionDocument;
import github.clone_code_detection.entity.highlight.document.HighlightSingleDocument;
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
public class HighlightSessionReportDTO {
    @JsonProperty
    private UUID id;

    @JsonProperty
    private Time created;

    @JsonProperty("matches")
    private Collection<HighlightSingleDocumentDTO> reports;

    public static HighlightSessionReportDTO from(HighlightSessionDocument sessionDocument) {
        HighlightSessionReportDTO sessionReportDTO = ModelMapperUtil.getMapper()
                                                                    .map(sessionDocument,
                                                                            HighlightSessionReportDTO.class);
        sessionReportDTO.reports = sessionDocument.getMatches()
                                                  .stream()
                                                  .map(HighlightSingleDocumentDTO::from)
                                                  .toList();
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
}
