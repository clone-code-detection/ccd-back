package github.clone_code_detection.entity.query;

import github.clone_code_detection.util.FileSystemUtil;
import github.clone_code_detection.util.LanguageUtil;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Builder
@Data
@AllArgsConstructor
public class QueryDocument {
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Nonnull
    private MultipartFile fileDocument;
    @Nonnull
    private String minimumShouldMatch; // Current feature api
    private Map<String, Object> queryMeta; // Other meta data for future api

    public String getLanguage() {
        return LanguageUtil.getInstance()
                           .getIndexFromFileName(fileDocument.getName());
    }

    public String getContent() {
        byte[] bytes = FileSystemUtil.getContent(fileDocument);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
