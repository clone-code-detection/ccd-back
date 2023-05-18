package github.clone_code_detection.entity.highlight.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.sql.Time;
import java.time.LocalTime;
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
    private Time created = Time.valueOf(LocalTime.now());
}
