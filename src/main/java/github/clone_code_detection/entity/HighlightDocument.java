package github.clone_code_detection.entity;

import lombok.Builder;
import lombok.Data;

import java.util.Collection;

@Data
@Builder
public class HighlightDocument {
    ElasticsearchDocument source;
    Collection<String> highlights;
}
