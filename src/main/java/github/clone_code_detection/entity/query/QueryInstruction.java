package github.clone_code_detection.entity.query;

import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.util.LanguageUtil;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Builder
@Data
@AllArgsConstructor
public class QueryInstruction {
    private Map<String, Object> queryMeta; // Other meta data for future api

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @NotNull
    private FileDocument queryDocument;

    @NotNull
    private String minimumShouldMatch; // Current feature api

    @NotNull
    private Boolean includeHighlight = false;

    public String getLanguage() {
        return LanguageUtil.getInstance()
                           .getIndexFromFileName(queryDocument.getFileName());
    }

    public String getContent() {
        byte[] bytes = queryDocument.getContent();
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
