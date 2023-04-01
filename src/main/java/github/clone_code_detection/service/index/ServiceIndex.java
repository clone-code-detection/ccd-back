package github.clone_code_detection.service.index;

import github.clone_code_detection.entity.ElasticsearchDocument;
import github.clone_code_detection.entity.index.IndexDocument;
import github.clone_code_detection.repo.RepoElasticsearchIndex;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.elasticsearch.action.bulk.BulkResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.stream.Stream;

@Service
public class ServiceIndex implements IServiceIndex {
    private final RepoElasticsearchIndex repoElasticsearchIndex;

    @Autowired
    public ServiceIndex(RepoElasticsearchIndex repoElasticsearchIndex) {
        this.repoElasticsearchIndex = repoElasticsearchIndex;
    }

    private static Pair<String, ElasticsearchDocument> apply(IndexDocument indexDocument) {
        ElasticsearchDocument elasticsearchDocument = ElasticsearchDocument.builder()
                                                                           .sourceCode(indexDocument.getContent())
                                                                           .build();
        String language = resolveLanguage(indexDocument.getLanguage());
        return Pair.of(language, elasticsearchDocument);
    }

    private static String resolveLanguage(String languageRequest) {
        return languageRequest;
    }

    @SneakyThrows
    @Override
    public BulkResponse indexAllDocuments(@NonNull Stream<IndexDocument> documents) {
        Stream<Pair<String, ElasticsearchDocument>> requestStream = documents.map(ServiceIndex::apply);
        return repoElasticsearchIndex.indexDocuments(requestStream);
    }
}
