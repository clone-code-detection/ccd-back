package github.clone_code_detection.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Map;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryDocument {
    private String content; // Content of that actual document
    private Collection<String> languages; // Which languages this document should be matched in
    private Map<String, Object> queryMeta; // Other meta data for future api
    private String minimumShouldMatch; // Current feature api
}
