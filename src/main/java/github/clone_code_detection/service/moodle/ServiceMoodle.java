package github.clone_code_detection.service.moodle;


import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.entity.highlight.request.HighlightSessionRequest;
import github.clone_code_detection.util.FileSystemUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
public class ServiceMoodle {

    private ServiceMoodle() {
        throw new IllegalStateException("This service only has static method");
    }

    public static Collection<HighlightSessionRequest> unzipMoodleFileAndGetRequests(MultipartFile file) throws IOException {
        // Moodle zip file has structure as list of folder, each folder is student's list file submissions
        ArrayList<HighlightSessionRequest> requests = new ArrayList<>();
        try {
            ZipInputStream zipInputStream = new ZipInputStream(file.getInputStream());
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            // Loop through each folder to create request
            while (zipEntry != null) {
                String[] array = zipEntry.getName().split("/");
                String author = array[0];
                String sessionName = String.format("%s_%s", FileSystemUtil.getFileName(file), author);
                String fileName = array[array.length - 1].substring(0, array[array.length - 1].lastIndexOf("."));
                HighlightSessionRequest request = getExistRequestOrCreateNew(requests, sessionName);
                // read content of folder into stream
                byteArrayOutputStream.reset();
                zipInputStream.transferTo(byteArrayOutputStream);
                FileDocument fileDocument = FileDocument
                        .builder()
                        .content(byteArrayOutputStream.toByteArray())
                        .fileName(fileName)
                        .build();
                request.getSources().add(fileDocument);
                zipEntry = zipInputStream.getNextEntry();
            }
            zipInputStream.closeEntry();
            zipInputStream.close();
        } catch (IOException e) {
            log.error("[Service moodle] Failed to unzip moodle zip file. Error: {}", e.getMessage());
            throw e;
        }
        return requests;
    }

    private static HighlightSessionRequest getExistRequestOrCreateNew(ArrayList<HighlightSessionRequest> requests, String sessionName) {
        for (HighlightSessionRequest request : requests) {
            if (request.getSessionName().equals(sessionName))
                return request;
        }
        requests.add(HighlightSessionRequest.builder().sessionName(sessionName).build());
        return requests.get(requests.size() - 1);
    }
}
