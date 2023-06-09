package github.clone_code_detection.entity.query;

import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.util.LanguageUtil;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;

@Builder
@Data
@AllArgsConstructor
public class QueryInstruction implements Cloneable {
    private Map<String, Object> queryMeta; // Other meta data for future api

    @NotNull
    private FileDocument queryDocument;

    @NotNull
    private Integer type;

    @NotNull
    private String minimumShouldMatch; // Current feature api

    public String getLanguage() {
        return LanguageUtil.getInstance()
                           .getIndexFromFileName(queryDocument.getFileName());
    }

    public String getContent() {
       return queryDocument.getContentAsString();
    }

    @Override
    public QueryInstruction clone() {
        try {
            QueryInstruction clone = (QueryInstruction) super.clone();
            clone.type = this.type;
            clone.minimumShouldMatch = this.minimumShouldMatch;
            clone.queryDocument = null;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
