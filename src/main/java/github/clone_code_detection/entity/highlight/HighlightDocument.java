package github.clone_code_detection.entity.highlight;

import github.clone_code_detection.entity.query.QueryDocument;
import lombok.Builder;
import lombok.Data;

import java.util.Collection;
import java.util.Map;

@Data
@Builder
public class HighlightDocument {
    String content; // Content of that actual document
    Collection<String> languages; // Which languages this document should be matched in
    Map<String, Object> queryMeta; // Other meta data for future api
    String minimumShouldMatch;
    HighlightMetadata metadata; // The extra information about highlight request

    public QueryDocument toQuery() {
        return QueryDocument.builder()
                .content(content)
                .languages(languages)
                .minimumShouldMatch(minimumShouldMatch)
                .queryMeta(queryMeta)
                .build();
    }
}
