package github.clone_code_detection.entity.highlight.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Builder
@Data
public class HighlightSingleMatchDTO {
    private UUID id;

    @JsonProperty("source")
    private String source;

    @JsonProperty("target")
    private String target;

    @JsonProperty("matches")
    @Builder.Default
    private List<HighlightWordMatchDTO> matches = new ArrayList<>();
}
