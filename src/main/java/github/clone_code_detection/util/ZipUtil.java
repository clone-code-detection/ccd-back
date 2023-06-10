package github.clone_code_detection.util;

import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.exceptions.UnsupportedLanguage;
import github.clone_code_detection.exceptions.highlight.FileHandleException;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
public class ZipUtil {
    private static final LanguageUtil languageUtil = LanguageUtil.getInstance();

    private ZipUtil() {
        throw new IllegalStateException("ZipUtil is utility class");
    }

    public static Collection<FileDocument> unzipAndGetContents(MultipartFile file) {
        ArrayList<FileDocument> contents = new ArrayList<>();
        try {
            ZipInputStream zipInputStream = new ZipInputStream(file.getInputStream());
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            while (zipEntry != null) {
                if (zipEntry.isDirectory()) {
                    zipEntry = zipInputStream.getNextEntry();
                    continue;
                }
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

    public static Collection<FileDocument> getFileDocumentFromZipFile(byte @NotNull [] bytes, @NotNull String author,
                                                                      Map<String, String> meta,
                                                                      String uri, String origin) {
        Collection<FileDocument> fileDocuments = new ArrayList<>();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        try (ZipInputStream zipInputStream = new ZipInputStream(byteArrayInputStream)) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (zipEntry.isDirectory())
                    continue;
                byteArrayOutputStream.reset();
                extracted(author, fileDocuments, byteArrayOutputStream, zipInputStream, zipEntry, meta, uri, origin);
            }
            zipInputStream.closeEntry();
        } catch (IOException e) {
            log.error("[Service moodle] Got error while unzip project file: {}", e.getMessage());
            return new ArrayList<>();
        }
        return fileDocuments;
    }

    private static void extracted(String author,
                                  Collection<FileDocument> fileDocuments,
                                  ByteArrayOutputStream byteArrayOutputStream,
                                  ZipInputStream zipInputStream,
                                  ZipEntry zipEntry, Map<String, String> meta, String uri, String origin) throws
                                                                                                          IOException {
        try {
            languageUtil.getIndexFromFileName(zipEntry.getName());
            zipInputStream.transferTo(byteArrayOutputStream);
            fileDocuments.add(FileDocument.builder()
                                          .author(author)
                                          .fileName(zipEntry.getName())
                                          .content(byteArrayOutputStream.toByteArray())
                                          .origin(origin)
                                          .originLink(uri)
                                          .meta(meta)
                                          .build());
        } catch (UnsupportedLanguage ignored) {}
    }
}
