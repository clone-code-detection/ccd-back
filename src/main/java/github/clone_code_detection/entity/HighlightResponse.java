package github.clone_code_detection.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.Collection;

@Data
@Builder
public class HighlightResponse {
    @JsonProperty("total_count")
    int totalCount;
    Collection<ElasticsearchDocument> documents;
    ElasticsearchDocument origin;
}
