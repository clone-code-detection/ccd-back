package github.clone_code_detection.service;

import github.clone_code_detection.entity.CrawlGitHubDocument;
import github.clone_code_detection.entity.index.IndexDocument;
import github.clone_code_detection.service.index.ServiceIndex;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
public class ServiceGitHub {
    private final ServiceIndex serviceIndex;

    @Autowired
    public ServiceGitHub(ServiceIndex serviceIndex) {
        this.serviceIndex = serviceIndex;
    }

    public Collection<String> unzipAndGetContents(MultipartFile file) {
        ArrayList<String> contents = new ArrayList<>();
        try {
            ZipInputStream zipInputStream = new ZipInputStream(file.getInputStream());
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            while (zipEntry != null) {
                byteArrayOutputStream.reset();
                zipInputStream.transferTo(byteArrayOutputStream);
                contents.add(byteArrayOutputStream.toString());
                zipEntry = zipInputStream.getNextEntry();
            }
            zipInputStream.closeEntry();
            zipInputStream.close();
        } catch (IOException e) {
            log.error("Error parsing zip file" , e);
            throw new RuntimeException(e);
        }
        return contents;
    }

    public BulkResponse buildRepositoryPayloads(Collection<String> contents ,
                                                CrawlGitHubDocument body) {
        Stream<IndexDocument> documents = contents
                .stream()
                .map(content -> new IndexDocument(content , body.getTarget() , body.getMeta()));
        return serviceIndex.indexAllDocuments(documents);
    }
}
