package github.clone_code_detection.entity.highlight;

import com.fasterxml.jackson.annotation.JsonProperty;
import github.clone_code_detection.entity.ElasticsearchDocument;
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
