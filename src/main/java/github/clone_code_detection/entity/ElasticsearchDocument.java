package github.clone_code_detection.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import github.clone_code_detection.entity.index.IndexDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
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

    @JsonProperty("_id")
    String id;

    public String asJson() throws JsonProcessingException {
        return mapper.writeValueAsString(this);
    }

    public static ElasticsearchDocument fromFileDocument(FileDocument fileDocument) {
        byte[] content = fileDocument.getContent();
        String contentAsString = new String(content, StandardCharsets.UTF_8);
        String esId = String.valueOf(fileDocument.getId());
        return ElasticsearchDocument.builder()
                                    .sourceCode(contentAsString)
                                    .id(esId)
                                    .build();
    }

    public static ElasticsearchDocument fromIndexDocument(IndexDocument indexDocument) {
        return ElasticsearchDocument.builder()
                                    .sourceCode(indexDocument.getContent())
                                    .build();
    }
}
