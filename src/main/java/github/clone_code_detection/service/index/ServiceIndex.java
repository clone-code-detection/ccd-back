package github.clone_code_detection.service.index;

import github.clone_code_detection.entity.ElasticsearchDocument;
import github.clone_code_detection.entity.FileDocument;
import github.clone_code_detection.entity.index.IndexInstruction;
import github.clone_code_detection.entity.index.IndexDocument;
import github.clone_code_detection.exceptions.highlight.ElasticsearchIndexException;
import github.clone_code_detection.exceptions.highlight.FileNotSupportedException;
import github.clone_code_detection.repo.RepoElasticsearchIndex;
import github.clone_code_detection.repo.RepoFileDocument;
import github.clone_code_detection.util.FileSystemUtil;
import github.clone_code_detection.util.LanguageUtil;
import github.clone_code_detection.util.ZipUtil;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.commons.io.FilenameUtils;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

@Service
public class ServiceIndex implements IServiceIndex {
    private final RepoElasticsearchIndex repoElasticsearchIndex;
    private final RepoFileDocument repoFile;

    @Autowired
    public ServiceIndex(RepoElasticsearchIndex repoElasticsearchIndex, RepoFileDocument repoFile) {
        this.repoElasticsearchIndex = repoElasticsearchIndex;
        this.repoFile = repoFile;
    }

    private static Pair<String, ElasticsearchDocument> apply(IndexDocument indexDocument) {
        ElasticsearchDocument elasticsearchDocument = ElasticsearchDocument.fromIndexDocument(indexDocument);
        String language = resolveLanguage(indexDocument.getLanguage());
        return Pair.of(language, elasticsearchDocument);
    }

    private static String resolveLanguage(String languageRequest) {
        return languageRequest;
    }

    /**
     * @param file
     * @implNote accept zip and single file
     */
    private void validate(MultipartFile file) {
        String fileName = file.getName();
        String extension = FilenameUtils.getExtension(fileName);
        if (extension.endsWith("zip")) {
            return;
        }
        try {
            LanguageUtil.getInstance()
                        .getIndexFromExtension(extension);
        } catch (Exception ignore) {
            throw new FileNotSupportedException(
                    MessageFormat.format("Extension of type {0} is not supported", extension));
        }
    }

    /**
     * @param file
     * @return
     * @implNote extract from zip if exists or single-content-collection
     */
    private Collection<FileDocument> extractDocuments(MultipartFile file) {
        String fileName = file.getName();
        String extension = FilenameUtils.getExtension(fileName);
        if (extension.endsWith("zip")) {
            return ZipUtil.unzipAndGetContents(file);
        } else {
            byte[] content;
            content = FileSystemUtil.getContent(file);
            return List.of(FileDocument.builder()
                                       .fileName(fileName)
                                       .content(content)
                                       .build());
        }
    }

    private static void validateBulkResponse(@Nonnull BulkResponse bulkResponse) {
        for (BulkItemResponse indexDocument : bulkResponse) {
            if (indexDocument.isFailed()) {
                throw new ElasticsearchIndexException(
                        MessageFormat.format("Failed to index document with id {0}", indexDocument.getId()));
            }
        }
    }

    @NonNull
    private static Pair<String, ElasticsearchDocument> getLangAndElasticsearchDoc(@Nonnull FileDocument fileDocument) {
        String index = LanguageUtil.getInstance()
                                   .getIndexFromFileName(fileDocument.getFileName());
        ElasticsearchDocument document = ElasticsearchDocument.fromFileDocument(fileDocument);
        return Pair.of(index, document);
    }

    @Override
    public void indexAllDocuments(MultipartFile file, IndexInstruction body) {
        validate(file);
        Collection<FileDocument> files = extractDocuments(file);
        // save to db
        files = repoFile.saveAll(files);
        //index
        Stream<Pair<String, ElasticsearchDocument>> stream = files.stream()
                                                                  .map(ServiceIndex::getLangAndElasticsearchDoc);
        try {
            BulkResponse bulkResponse = repoElasticsearchIndex.indexDocuments(stream);
            validateBulkResponse(bulkResponse);
        } catch (IOException e) {
            throw new ElasticsearchIndexException("Failed to index documents");
        }
    }

    @SneakyThrows
    @Override
    public BulkResponse indexAllDocuments(@NonNull Stream<IndexDocument> documents) {
        Stream<Pair<String, ElasticsearchDocument>> requestStream = documents.map(ServiceIndex::apply);
        return repoElasticsearchIndex.indexDocuments(requestStream);
    }
}
