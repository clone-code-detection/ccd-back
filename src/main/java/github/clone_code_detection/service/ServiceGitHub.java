package github.clone_code_detection.service;

import co.elastic.clients.elasticsearch.core.BulkResponse;
import github.clone_code_detection.entity.CrawlGitHubDocument;
import github.clone_code_detection.entity.IndexDocument;
import github.clone_code_detection.entity.SourceCodeDocument;
import github.clone_code_detection.util.UtilLanguageMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
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
    private final UtilLanguageMap utilLanguageMap;

    @Autowired
    public ServiceGitHub(ServiceIndex serviceIndex, UtilLanguageMap utilLanguageMap) {
        this.serviceIndex = serviceIndex;
        this.utilLanguageMap = utilLanguageMap;
    }

    public Collection<SourceCodeDocument> unzipAndGetContents(MultipartFile file) {
        ArrayList<SourceCodeDocument> contents = new ArrayList<>();
        try {
            ZipInputStream zipInputStream = new ZipInputStream(file.getInputStream());
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            while (zipEntry != null) {
                byteArrayOutputStream.reset();
                zipInputStream.transferTo(byteArrayOutputStream);

                SourceCodeDocument sourceCodeDocument = getSourceCodeDocument(zipEntry, byteArrayOutputStream);
                contents.add(sourceCodeDocument);
                zipEntry = zipInputStream.getNextEntry();
            }
            zipInputStream.closeEntry();
            zipInputStream.close();
        } catch (IOException e) {
            log.error("Error parsing zip file", e);
            throw new RuntimeException(e);
        }
        return contents;
    }

    private SourceCodeDocument getSourceCodeDocument(ZipEntry zipEntry, ByteArrayOutputStream byteArrayOutputStream) {
        String content = byteArrayOutputStream.toString();
        String extension = FilenameUtils.getExtension(zipEntry.getName());
        return new SourceCodeDocument(content, extension);
    }

    public BulkResponse buildRepositoryPayloads(Collection<SourceCodeDocument> contents, CrawlGitHubDocument body) {
        Stream<IndexDocument> documents = contents.stream()
                                                  .map(content -> {
                                                      String indexName = utilLanguageMap.getIndexFromExtension(
                                                              content.getExtension());
                                                      return new IndexDocument(content.getContent(), indexName,
                                                              body.getMeta());
                                                  });
        return serviceIndex.indexAllDocuments(documents);
    }
}
