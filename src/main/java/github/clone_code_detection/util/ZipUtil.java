package github.clone_code_detection.util;

import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.exceptions.highlight.FileHandleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
public class ZipUtil {
    public static Collection<FileDocument> unzipAndGetContents(MultipartFile file) {
        ArrayList<FileDocument> contents = new ArrayList<>();
        try {
            ZipInputStream zipInputStream = new ZipInputStream(file.getInputStream());
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            while (zipEntry != null) {
                byteArrayOutputStream.reset();
                zipInputStream.transferTo(byteArrayOutputStream);
                FileDocument fileDocument = FileDocument.builder()
                                                        .content(byteArrayOutputStream.toByteArray())
                                                        .fileName(zipEntry.getName())
                                                        .build();
                contents.add(fileDocument);
                zipEntry = zipInputStream.getNextEntry();
            }
            zipInputStream.closeEntry();
            zipInputStream.close();
        } catch (IOException e) {
            throw new FileHandleException("Error parsing zip file");
        }
        return contents;
    }
}
