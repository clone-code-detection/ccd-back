package github.clone_code_detection.entity.index;

import github.clone_code_detection.entity.FileDocument;
import github.clone_code_detection.util.LanguageUtil;
import lombok.*;

import javax.annotation.Nonnull;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class IndexDocument {
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Nonnull
    private FileDocument fileDocument;
    private Map<String, Object> meta;
    // All other fields, like location of that document in storage
    // Author
    // Created time
    // Crawled time

    public String getLanguage() {
        return LanguageUtil.getInstance()
                           .getIndexFromFileName(fileDocument.getFileName());
    }

    public String getContent() {
        return fileDocument.getContent();
    }
}
