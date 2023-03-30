package github.clone_code_detection.service;

import co.elastic.clients.elasticsearch.core.BulkResponse;
import github.clone_code_detection.entity.index.IndexDocument;

import java.util.stream.Stream;

public interface IServiceIndex {
    BulkResponse indexAllDocuments(Stream<IndexDocument> documents);
}
