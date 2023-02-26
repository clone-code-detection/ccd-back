package github.clone_code_detection.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ElasticsearchDocument {
    @JsonProperty("meta")
    Map<String, String> meta;

    @JsonProperty("source_code")
    String sourceCode;
}
