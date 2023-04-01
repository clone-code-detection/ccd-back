package github.clone_code_detection.entity.index;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexDocument {
    private String content; // Content of that actual document
    private String language; // Which language this document should be indexed for
    private Map<String, Object> meta;
    // All other fields, like location of that document in storage
    // Author
    // Created time
    // Crawled time
}
