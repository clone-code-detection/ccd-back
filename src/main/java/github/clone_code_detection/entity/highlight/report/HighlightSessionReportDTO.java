package github.clone_code_detection.entity.highlight.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import github.clone_code_detection.entity.highlight.document.HighlightSessionDocument;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HighlightSessionReportDTO {
    @JsonProperty(value = "session_id")
    private UUID sessionId;

    @JsonProperty(value = "session_name")
    private String sessionName;

    @JsonProperty(value = "created_time")
    @Builder.Default
    @Setter(AccessLevel.NONE)
    private LocalDateTime created = LocalDateTime.now();

    public static HighlightSessionReportDTO from(HighlightSessionDocument document) {
        return HighlightSessionReportDTO.builder()
                                 .sessionId(document.getId())
                                 .sessionName(document.getName())
                                 .build();
    }
}
