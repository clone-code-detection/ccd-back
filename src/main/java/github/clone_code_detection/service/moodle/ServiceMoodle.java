package github.clone_code_detection.service.moodle;


import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.entity.highlight.request.HighlightSessionRequest;
import github.clone_code_detection.exceptions.UnsupportedLanguage;
import github.clone_code_detection.util.FileSystemUtil;
import github.clone_code_detection.util.LanguageUtil;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
public class ServiceMoodle {

    private static final LanguageUtil languageUtil = LanguageUtil.getInstance();

    private ServiceMoodle() {
        throw new IllegalStateException("This service only has static method");
    }

    public static Collection<HighlightSessionRequest> unzipMoodleFileAndGetRequests(@NotNull MultipartFile source) throws IOException {
        // Moodle zip file has structure as list of folder, each folder is student's list file submissions
        ArrayList<HighlightSessionRequest> requests = new ArrayList<>(); // Each request will be the highlight session
        // Create zip handler for root zip file which got grom moodle
        try (ZipInputStream zipInputStream = new ZipInputStream(source.getInputStream())) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ZipEntry zipEntry;
            // Loop over each student submission file, which can be source code file or zip file
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                // If entry is the source code file then create new session
                String author = zipEntry.getName().split("/")[0];
                String sessionName = String.format("%s/%s", FileSystemUtil.getFileName(source.getOriginalFilename()), FileSystemUtil.getFileName(zipEntry.getName()));
                if (zipEntry.isDirectory()) {
//                    log.info("[Service moodle] Ignore because zip entry {} is folder", zipEntry.getName());
                    continue;
                }
                byteArrayOutputStream.reset();
                HighlightSessionRequest request = getExistRequestOrCreateNew(requests, sessionName); // get the exists highlight session request or new request which has been added into Collection

                Collection<FileDocument> fileDocuments = new ArrayList<>();
                zipInputStream.transferTo(byteArrayOutputStream);
                switch (FilenameUtils.getExtension(zipEntry.getName())) {
                    case "" -> log.warn("[Service moodle] File {} has empty extension", zipEntry.getName());
                    // Case nested zip file inside root file
                    case "zip" -> {
//                        log.info("[Service moodle] handle nested zip file {}", zipEntry.getName());
                        fileDocuments.addAll(getFileDocumentFromNestedZipFile(byteArrayOutputStream.toByteArray(), author));
                    }
                    default -> {
//                        log.info("[Service moodle] Handle source code file {}", zipEntry.getName());
                        try {
                            languageUtil.getIndexFromFileName(zipEntry.getName());
                            // For source code file, only 1 file document is created
                            fileDocuments.add(FileDocument
                                    .builder()
                                    .content(byteArrayOutputStream.toByteArray())
                                    .fileName(zipEntry.getName())
                                    .author(author)
                                    .build());
                        } catch (UnsupportedLanguage e) {
                            log.warn("[Service moodle] File {} is ignored because its language isn't supported", zipEntry.getName());
                        }

                    }
                }
                if (!fileDocuments.isEmpty())
                    request.setSources(fileDocuments);
            }
        }

        return requests;
    }

    private static Collection<FileDocument> getFileDocumentFromNestedZipFile(@NotNull byte[] bytes, @NotNull String author) {
        Collection<FileDocument> fileDocuments = new ArrayList<>();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ByteArrayInputStream byteArrayInputStream= new ByteArrayInputStream(bytes);
        try (ZipInputStream zipInputStream = new ZipInputStream(byteArrayInputStream)) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (zipEntry.isDirectory()) {
//                    log.warn("[Service moodle] Entry {} is ignored because it is folder", zipEntry.getName());
                    continue;
                }
                byteArrayOutputStream.reset();
                extracted(author, fileDocuments, byteArrayOutputStream, zipInputStream, zipEntry);
            }
            zipInputStream.closeEntry();
        } catch (IOException e) {
            log.error("[Service moodle] Got error while unzip project file: {}", e.getMessage());
            return new ArrayList<>();
        }
        return fileDocuments;
    }

    private static void extracted(String author, Collection<FileDocument> fileDocuments, ByteArrayOutputStream byteArrayOutputStream, ZipInputStream zipInputStream, ZipEntry zipEntry) throws IOException {
        try {
            languageUtil.getIndexFromFileName(zipEntry.getName());
            zipInputStream.transferTo(byteArrayOutputStream);
            fileDocuments.add(FileDocument
                    .builder()
                    .author(author)
                    .fileName(zipEntry.getName())
                    .content(byteArrayOutputStream.toByteArray())
                    .build());
        } catch (UnsupportedLanguage e) {
            log.warn("[Service moodle] File {} is ignored because its language isn't supported", zipEntry.getName());
        }
    }

    private static HighlightSessionRequest getExistRequestOrCreateNew(ArrayList<HighlightSessionRequest> requests, String sessionName) {
        for (HighlightSessionRequest request : requests) {
            if (request.getSessionName().equals(sessionName))
                return request;
        }
        HighlightSessionRequest request = HighlightSessionRequest.builder().sessionName(sessionName).build();
        requests.add(request);
        return request;
    }
}
