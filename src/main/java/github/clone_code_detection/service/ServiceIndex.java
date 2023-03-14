package github.clone_code_detection.service;

import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import github.clone_code_detection.entity.ElasticsearchDocument;
import github.clone_code_detection.entity.IndexDocument;
import github.clone_code_detection.repo.RepoElasticsearchIndex;
import github.clone_code_detection.util.UtilLanguageMap;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class ServiceIndex implements IServiceIndex {
    private final RepoElasticsearchIndex repoElasticsearchIndex;

    @Autowired
    public ServiceIndex(RepoElasticsearchIndex repoElasticsearchIndex) {
        this.repoElasticsearchIndex = repoElasticsearchIndex;
    }

    private String resolveLanguage(String languageRequest) {
        return languageRequest;
    }

    @SneakyThrows
    @Override
    public BulkResponse indexAllDocuments(Stream<IndexDocument> documents) {
        Stream<IndexRequest<ElasticsearchDocument>> requestStream = documents.map(indexDocument -> {
            String language = indexDocument.getLanguage();
            ElasticsearchDocument value = ElasticsearchDocument.builder()
                                                               .sourceCode(indexDocument.getContent())
                                                               .build();
            return IndexRequest.of(ir -> ir.index(language)
                                           .document(value));
        });
        return repoElasticsearchIndex.indexDocuments(requestStream);
    }
}
