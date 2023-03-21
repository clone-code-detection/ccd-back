package github.clone_code_detection.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HighlightResponse {
    @JsonProperty("total_count")
    int totalCount;
    Collection<ElasticsearchDocument> documents;
    ElasticsearchDocument origin;
}
