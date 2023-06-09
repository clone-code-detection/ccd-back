package github.clone_code_detection.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import github.clone_code_detection.entity.fs.FileDocument;
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

    @JsonProperty("source_code_normalized")
    String sourceCodeNormalized;

    @JsonIgnore
    String id;

    public String asJson() throws JsonProcessingException {
        return mapper.writeValueAsString(this);
    }

    public static ElasticsearchDocument fromFileDocument(FileDocument fileDocument) {
        String contentAsString = fileDocument.getContentAsString();
        String esId = String.valueOf(fileDocument.getId());
        return ElasticsearchDocument.builder()
                                    .sourceCode(contentAsString)
                                    .sourceCodeNormalized(contentAsString)
                                    .id(esId)
                                    .build();
    }
}
