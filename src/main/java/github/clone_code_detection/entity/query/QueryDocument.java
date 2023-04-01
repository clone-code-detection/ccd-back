package github.clone_code_detection.entity.query;

import github.clone_code_detection.entity.FileDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryDocument {
    private FileDocument fileDocument;
    private Map<String, Object> queryMeta; // Other meta data for future api
    private String minimumShouldMatch; // Current feature api

    public String getLanguage() {
        return fileDocument.getFileName();
    }
}
