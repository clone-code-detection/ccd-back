package github.clone_code_detection.service;

import github.clone_code_detection.entity.CrawlGitHubDocument;
import github.clone_code_detection.entity.FileDocument;
import github.clone_code_detection.entity.index.IndexDocument;
import github.clone_code_detection.service.index.ServiceIndex;
import github.clone_code_detection.util.ZipUtil;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.stream.Stream;

@Slf4j
@Service
public class ServiceGitHub {
    private final ServiceIndex serviceIndex;

    @Autowired
    public ServiceGitHub(ServiceIndex serviceIndex) {
        this.serviceIndex = serviceIndex;
    }

    public BulkResponse buildRepositoryPayloads(MultipartFile file, CrawlGitHubDocument body) {
        Collection<FileDocument> files = ZipUtil.unzipAndGetContents(file);
        Stream<IndexDocument> documents = files.stream()
                                               .map(content -> IndexDocument.builder()
                                                                            .fileDocument(content)
                                                                            .meta(body.getMeta())
                                                                            .build());
        return serviceIndex.indexAllDocuments(documents);
    }
}
