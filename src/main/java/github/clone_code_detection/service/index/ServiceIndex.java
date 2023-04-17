package github.clone_code_detection.service.index;

import github.clone_code_detection.entity.ElasticsearchDocument;
import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.entity.index.IndexInstruction;
import github.clone_code_detection.exceptions.highlight.ElasticsearchIndexException;
import github.clone_code_detection.exceptions.highlight.FileNotSupportedException;
import github.clone_code_detection.repo.RepoElasticsearchIndex;
import github.clone_code_detection.repo.RepoFileDocument;
import github.clone_code_detection.util.FileSystemUtil;
import github.clone_code_detection.util.LanguageUtil;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
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
import java.util.stream.Stream;

@Service
@Slf4j
public class ServiceIndex implements IServiceIndex {
    private final RepoElasticsearchIndex repoElasticsearchIndex;
    private final RepoFileDocument repoFile;

    @Autowired
    public ServiceIndex(RepoElasticsearchIndex repoElasticsearchIndex, RepoFileDocument repoFile) {
        this.repoElasticsearchIndex = repoElasticsearchIndex;
        this.repoFile = repoFile;
    }

    @NonNull
    private static Pair<String, ElasticsearchDocument> getLangAndElasticsearchDoc(@Nonnull FileDocument fileDocument) {
        String index = LanguageUtil.getInstance()
                                   .getIndexFromFileName(fileDocument.getFileName());
        ElasticsearchDocument document = ElasticsearchDocument.fromFileDocument(fileDocument);
        return Pair.of(index, document);
    }

    /**
     * @param file
     * @implNote accept zip and single file
     */
    private void validate(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(fileName);
        assert extension != null;
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

    private static void validateBulkResponse(@Nonnull BulkResponse bulkResponse) {
        for (BulkItemResponse indexDocument : bulkResponse) {
            if (indexDocument.isFailed()) {
                log.info("[Service index] index single document: {}", indexDocument.getFailureMessage());
                throw new ElasticsearchIndexException(
                        MessageFormat.format("Failed to index document with id {0}", indexDocument.getId()));
            }
        }
    }

    @Override
    public Collection<FileDocument> indexAllDocuments(MultipartFile file, IndexInstruction body) {
        validate(file);
        Collection<FileDocument> files = FileSystemUtil.extractDocuments(file);
        body.setFiles(files);
        return this.indexAllDocuments(body);
    }

    public Collection<FileDocument> indexAllDocuments(IndexInstruction instruction) {
//        UserImpl user = ServiceHighlight.getUserFromContext();
        // save to db
        Collection<FileDocument> files = instruction.getFiles();
//        for (FileDocument file : files) {
//            file.setUser(user);
//        }
        files = repoFile.saveAll(files);
        //index
        Stream<Pair<String, ElasticsearchDocument>> stream = files.stream()
                                                                  .map(ServiceIndex::getLangAndElasticsearchDoc);
        try {
            BulkResponse bulkResponse = repoElasticsearchIndex.indexDocuments(stream);
            validateBulkResponse(bulkResponse);
        } catch (IOException e) {
            log.error("[Service index] index documents", e);
            throw new ElasticsearchIndexException("Failed to index documents");
        }
        return files;
    }
}
