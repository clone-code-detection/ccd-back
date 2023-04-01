package github.clone_code_detection.service.highlight;

import github.clone_code_detection.entity.FileDocument;
import github.clone_code_detection.exceptions.highlight.FileHandleException;
import github.clone_code_detection.exceptions.highlight.FileNotSupportedException;
import github.clone_code_detection.util.LanguageUtil;
import github.clone_code_detection.util.ZipUtil;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;

@Service
@Validated
public class ServiceHighlight2 {
    public void validate(MultipartFile file) {
        String fileName = file.getName();
        String extension = FilenameUtils.getExtension(fileName);
        if (extension.endsWith("zip")) {
            return;
        }
        try {
            LanguageUtil.getInstance()
                        .getIndexFromExtension(extension);
        } catch (Exception ignore) {
            throw new FileNotSupportedException(
                    MessageFormat.format("Extension of type {0} is not supported", extension));
        }
    }

    public Collection<FileDocument> extractDocuments(MultipartFile file) {
        String fileName = file.getName();
        String extension = FilenameUtils.getExtension(fileName);
        if (extension.endsWith("zip")) {
            return ZipUtil.unzipAndGetContents(file);
        } else {
            String content;
            try {
                content = new String(file.getInputStream()
                                         .readAllBytes(), StandardCharsets.UTF_8);
                return List.of(FileDocument.builder()
                                           .fileName(fileName)
                                           .content(content)
                                           .build());
            } catch (IOException e) {
                throw new FileHandleException("Error parsing file content");
            }
        }
    }

    public void test(@Nonnull MultipartFile file) {
        validate(file);
        Collection<FileDocument> documents = extractDocuments(file);
    }
}