package github.clone_code_detection.entity.highlight.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Time;
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
    private Time created;
}
