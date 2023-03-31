package github.clone_code_detection.entity;

import lombok.Builder;
import lombok.Data;

import java.util.Collection;
import java.util.Map;

@Data
@Builder
public class CrawlGitHubDocument {
    private Collection<String> target;
    private Map<String, Object> meta;
}
