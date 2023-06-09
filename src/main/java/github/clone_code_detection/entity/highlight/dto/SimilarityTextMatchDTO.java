package github.clone_code_detection.entity.highlight.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Builder
@Data
public class SimilarityTextMatchDTO {
    @JsonProperty("word")
    private String text;
    @JsonProperty("target")
    @Builder.Default
    private List<Integer[]> targetMatches = new ArrayList<>();
    @JsonProperty("source")
    @Builder.Default
    private List<Integer[]> sourceMatches = new ArrayList<>();
}
