package github.clone_code_detection.service;

import github.clone_code_detection.entity.CrawlGitHubDocument;
import github.clone_code_detection.entity.IndexDocument;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ServiceGitHub {
    public static Collection<String> unzipAndGetContents(MultipartFile file) {
        byte[] buffer = new byte[1024];
        ArrayList<String> contents = new ArrayList<>();
        try {
            ZipInputStream zipInputStream = new ZipInputStream(file.getInputStream());
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                StringBuilder content = new StringBuilder();
                int length;
                while ((length = zipInputStream.read(buffer)) > 0) {
                    content.append(new String(buffer, 0, length));
                }
                zipInputStream.closeEntry();
                zipEntry = zipInputStream.getNextEntry();
                contents.add(content.toString());
            }
            zipInputStream.closeEntry();
            zipInputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return contents;
    }

    public static Collection<IndexDocument> buildRepositoryPayloads(Collection<String> contents, CrawlGitHubDocument body) {
        return contents.stream().map(content -> new IndexDocument(content, body.getTarget(), body.getMeta())).collect(Collectors.toCollection(ArrayList::new));
    }
}
