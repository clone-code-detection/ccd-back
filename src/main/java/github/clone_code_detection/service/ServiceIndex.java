package github.clone_code_detection.service;

import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import github.clone_code_detection.entity.ElasticsearchDocument;
import github.clone_code_detection.entity.IndexDocument;
import github.clone_code_detection.repo.RepoElasticsearchIndex;
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
        Stream<IndexRequest<ElasticsearchDocument>> requestStream =
                documents.flatMap(indexDocument -> {
                    Collection<String> languages = indexDocument.getLanguages();
                    ElasticsearchDocument value = ElasticsearchDocument.builder()
                            .sourceCode(indexDocument.getContent())
                            .meta(indexDocument.getMeta())
                            .build();
                    return languages.stream()
                            .map(this::resolveLanguage)
                            .filter(Objects::nonNull)
                            .map(lang -> IndexRequest.of(ir -> ir.index(lang)
                                    .document(value)));
                });
        return repoElasticsearchIndex.indexDocuments(requestStream);
    }
}
