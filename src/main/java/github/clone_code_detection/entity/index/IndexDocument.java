package github.clone_code_detection.entity.index;

import github.clone_code_detection.util.FileSystemUtil;
import github.clone_code_detection.util.LanguageUtil;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class IndexDocument {
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Nonnull
    private MultipartFile fileDocument;
    private Map<String, Object> meta;
    // All other fields, like location of that document in storage
    // Author
    // Created time
    // Crawled time

    public String getLanguage() {
        return LanguageUtil.getInstance()
                           .getIndexFromFileName(fileDocument.getName());
    }

    public String getContent() {
        byte[] bytes = FileSystemUtil.getContent(fileDocument);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
