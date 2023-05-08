package github.clone_code_detection.entity.moodle;

import com.fasterxml.jackson.annotation.JsonProperty;
import github.clone_code_detection.entity.highlight.report.HighlightSessionReportDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoodleResponse {
    @JsonProperty(value = "message", required = true)
    private String message;

    @JsonProperty(value = "data")
    private Collection<MoodleFolder> data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class MoodleFolder {
        @JsonProperty(value = "name")
        private String name;

        @JsonProperty(value = "sessions")
        private Collection<HighlightSessionReportDTO> sessions;
    }
}
