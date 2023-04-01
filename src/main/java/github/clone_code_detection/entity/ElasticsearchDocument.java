package github.clone_code_detection.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static final ObjectMapper mapper = new ObjectMapper();

    @JsonProperty("meta")
    Map<String, String> meta;

    @JsonProperty("source_code")
    String sourceCode;

    public String asJson() throws JsonProcessingException {
        return mapper.writeValueAsString(this);
    }
}
