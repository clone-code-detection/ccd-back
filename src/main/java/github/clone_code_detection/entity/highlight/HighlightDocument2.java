package github.clone_code_detection.entity.highlight;

import lombok.Builder;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
public class HighlightDocument2 {
    private MultipartFile file;
}
