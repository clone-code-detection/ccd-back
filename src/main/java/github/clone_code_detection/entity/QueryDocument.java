package github.clone_code_detection.entity;

import java.util.Collection;
import java.util.Map;

public class QueryDocument {
    private String content; // Content of that actual document
    private Collection<String> languages; // Which languages this document should be matched in
    private Map<String, Object> queryMeta; // Other meta data for future api
    private Double minimumShouldMatch; // Current feature api
}
