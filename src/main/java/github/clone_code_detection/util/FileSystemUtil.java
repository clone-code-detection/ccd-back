package github.clone_code_detection.util;

import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.exceptions.highlight.FileNotSupportedException;
import org.apache.commons.io.FilenameUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class FileSystemUtil {
    public static byte[] getContent(MultipartFile file) {
        try {
            return file.getInputStream()
                    .readAllBytes();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    /**
     * @param file the source code file or zip file
     * @return Collection of file document extracted from file
     * @implNote extract from zip if exists or single-content-collection
     */
    public static Collection<FileDocument> extractDocuments(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(fileName);
        assert extension != null;
        if (extension.endsWith("zip")) {
            return ZipUtil.unzipAndGetContents(file);
        } else {
            byte[] content;
            content = FileSystemUtil.getContent(file);
            return List.of(FileDocument.builder()
                    .fileName(fileName)
                    .content(content)
                    .build());
        }
    }

    /**
     * @param file the zip file or source code file
     * @implNote accept zip and single file
     */
    public static void validate(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(fileName);
        assert extension != null;
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

    /**
     * @param source the source code file from http request
     * @return name of that file without extension
     */
    public static String getFileName(MultipartFile source) {
        return Objects.requireNonNull(source.getOriginalFilename()).substring(0, source.getOriginalFilename().lastIndexOf("."));
    }
}
