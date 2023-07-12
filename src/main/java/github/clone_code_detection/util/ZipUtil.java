package github.clone_code_detection.util;

import com.fasterxml.jackson.databind.JsonNode;
import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.exceptions.file.FailHandleException;
import github.clone_code_detection.exceptions.file.UnsupportedLanguageException;
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

    public static Collection<FileDocument> unzipAndGetContents(MultipartFile file, JsonNode meta) {
        ArrayList<FileDocument> contents = new ArrayList<>();
        try {
            ZipInputStream zipInputStream = new ZipInputStream(file.getInputStream());
            ZipEntry zipEntry;
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (zipEntry.isDirectory()) {
                    zipInputStream.closeEntry();
                    continue;
                }
                // Ignore file if the language is not supported
                try {
                    languageUtil.getIndexFromFileName(zipEntry.getName());
                } catch (UnsupportedLanguageException e) {
                    zipInputStream.closeEntry();
                    continue;
                }
                byteArrayOutputStream.reset();
                zipInputStream.transferTo(byteArrayOutputStream);
                String filename = zipEntry.getName();
                int slashIndex = zipEntry.getName().lastIndexOf("/");
                if (slashIndex > -1)
                    filename = zipEntry.getName().substring(slashIndex + 1);
                FileDocument fileDocument = FileDocument.builder()
                                                        .content(byteArrayOutputStream.toByteArray())
                                                        .fileName(filename)
                                                        .author(meta.get("author").asText("anonymous"))
                                                        .origin(meta.get("origin").asText("local"))
                                                        .originLink(meta.get("origin_link").asText(""))
                                                        .build();
                contents.add(fileDocument);
            }
            zipInputStream.closeEntry();
            zipInputStream.close();
        } catch (IOException e) {
            throw new FailHandleException("Error parsing zip file");
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
        } catch (UnsupportedLanguageException ignored) {
        }
    }
}
