package github.clone_code_detection.util;

import com.fasterxml.jackson.databind.JsonNode;
import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.exceptions.file.UnsupportedLanguageException;
import org.apache.commons.io.FilenameUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class FileSystemUtil {

    private FileSystemUtil() {
        throw new IllegalStateException("FileSystemUtil is utility class");
    }

    public static byte[] getContent(MultipartFile file) {
        try {
            return file.getInputStream().readAllBytes();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    /**
     * @param file the source code file or zip file
     * @param meta the file metadata like author, origin, ...
     * @return Collection of file document extracted from file
     * @implNote extract from zip if exists or single-content-collection
     */
    public static Collection<FileDocument> extractDocuments(MultipartFile file, JsonNode meta) {
        String fileName = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(fileName);
        assert extension != null;
        if (extension.endsWith("zip")) {
            return ZipUtil.unzipAndGetContents(file, meta);
        } else {
            byte[] content;
            content = FileSystemUtil.getContent(file);
            return List.of(FileDocument.builder()
                                       .fileName(fileName)
                                       .content(content)
                                       .author(meta.get("author").asText("anonymous"))
                                       .origin(meta.get("origin").asText("local"))
                                       .originLink(meta.get("origin_link").asText(""))
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
            LanguageUtil.getInstance().getIndexFromExtension(extension);
        } catch (Exception ignore) {
            throw new UnsupportedLanguageException(MessageFormat.format("Extension of type {0} is not supported",
                                                                        extension));
        }
    }

    /**
     * @param source the source code file from http request
     * @return name of that file without extension
     */
    public static String getFileName(MultipartFile source) {
        String filename = Objects.requireNonNull(source.getOriginalFilename());
        return filename.substring(0, filename.lastIndexOf("."));
    }

    /**
     * @param originalFilename the full name with path and extension of file
     * @return name of that file without extension
     */
    public static String getFileName(String originalFilename) {
        if (originalFilename == null) return "";
        return originalFilename.substring(0, originalFilename.lastIndexOf("."));
    }
}
