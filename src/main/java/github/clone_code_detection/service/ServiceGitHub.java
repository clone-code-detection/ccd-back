package github.clone_code_detection.service;

import co.elastic.clients.elasticsearch.core.BulkResponse;
import github.clone_code_detection.entity.CrawlGitHubDocument;
import github.clone_code_detection.entity.ElasticsearchDocument;
import github.clone_code_detection.entity.IndexDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
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

    public Collection<ElasticsearchDocument> unzipAndGetContents(MultipartFile file) {
        ArrayList<ElasticsearchDocument> contents = new ArrayList<>();
        try {
            ZipInputStream zipInputStream = new ZipInputStream(file.getInputStream());
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            while (zipEntry != null) {
                String[] params = zipEntry.getName().split("~");
                Map<String, String> meta = new HashMap<>(){{
                   put("directory", params[0]);
                   put("sub_directory", params[1]);
                   put("start_line", params[2]);
                   put("end_line", params[3]);
                   put("filename", params[4]);
                }};
                byteArrayOutputStream.reset();
                zipInputStream.transferTo(byteArrayOutputStream);
                contents.add(ElasticsearchDocument.builder().sourceCode(byteArrayOutputStream.toString()).meta(meta).build());
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

    public BulkResponse buildRepositoryPayloads(Collection<ElasticsearchDocument> contents ,
                                                CrawlGitHubDocument body) {
        Stream<IndexDocument> documents = contents
                .stream()
                .map(content -> new IndexDocument(content.getSourceCode() , body.getTarget() , content.getMeta()));
        return serviceIndex.indexAllDocuments(documents);
    }
}
