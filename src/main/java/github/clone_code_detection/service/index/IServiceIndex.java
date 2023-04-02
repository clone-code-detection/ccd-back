package github.clone_code_detection.service.index;

import github.clone_code_detection.entity.index.IndexInstruction;
import github.clone_code_detection.entity.index.IndexDocument;
import org.elasticsearch.action.bulk.BulkResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

@Validated
public interface IServiceIndex {
    BulkResponse indexAllDocuments(@Nonnull Stream<IndexDocument> documents);

    void indexAllDocuments(MultipartFile file, IndexInstruction body);
}
