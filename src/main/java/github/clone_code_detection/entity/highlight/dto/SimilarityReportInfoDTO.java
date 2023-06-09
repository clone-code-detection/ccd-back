package github.clone_code_detection.entity.highlight.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import github.clone_code_detection.entity.highlight.document.SimilarityReport;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimilarityReportInfoDTO {
    @JsonProperty(value = "session_id")
    private UUID sessionId;

    @JsonProperty(value = "report_name")
    private String reportName;

    @JsonProperty(value = "created_time")
    @Builder.Default
    @Setter(AccessLevel.NONE)
    private LocalDateTime created = LocalDateTime.now();

    public static SimilarityReportInfoDTO from(SimilarityReport report) {
        return SimilarityReportInfoDTO.builder().sessionId(report.getId()).reportName(report.getName()).build();
    }
}
