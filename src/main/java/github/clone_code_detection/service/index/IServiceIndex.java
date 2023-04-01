package github.clone_code_detection.service.index;

import github.clone_code_detection.entity.index.IndexDocument;
import org.elasticsearch.action.bulk.BulkResponse;

import java.util.stream.Stream;

public interface IServiceIndex {
    BulkResponse indexAllDocuments(Stream<IndexDocument> documents);
}
