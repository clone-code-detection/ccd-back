package github.clone_code_detection.util;

import github.clone_code_detection.entity.fs.FileDocument;
import org.apache.commons.io.FilenameUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class FileSystemUtil {
    public static void saveFileToLocal(MultipartFile file) {

    }

    public static byte[] getContent(MultipartFile file) {
        try {
            return file.getInputStream()
                       .readAllBytes();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    /**
     * @param file
     * @return
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
}
