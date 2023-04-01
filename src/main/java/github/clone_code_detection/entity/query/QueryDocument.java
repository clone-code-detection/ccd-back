package github.clone_code_detection.entity.query;

import github.clone_code_detection.entity.FileDocument;
import github.clone_code_detection.util.LanguageUtil;
import lombok.*;

import javax.annotation.Nonnull;
import java.util.Map;

@Builder
@Data
@AllArgsConstructor
public class QueryDocument {
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Nonnull
    private FileDocument fileDocument;
    @Nonnull
    private String minimumShouldMatch; // Current feature api
    private Map<String, Object> queryMeta; // Other meta data for future api

    public String getLanguage() {
        return LanguageUtil.getInstance()
                           .getIndexFromFileName(fileDocument.getFileName());
    }

    public String getContent() {
        return fileDocument.getContent();
    }
}
