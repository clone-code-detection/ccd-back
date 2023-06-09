package github.clone_code_detection.entity.moodle;

import com.fasterxml.jackson.annotation.JsonProperty;
import github.clone_code_detection.entity.highlight.dto.SimilarityReportInfoDTO;
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
    private Collection<SimilarityReportInfoDTO> data;
}

