package github.clone_code_detection.service;

import co.elastic.clients.elasticsearch.core.IndexResponse;
import github.clone_code_detection.entity.IndexDocument;

import java.util.Collection;
import java.util.stream.Stream;

public interface IServiceIndex {
    Collection<IndexResponse> indexAllDocuments(Stream<IndexDocument> documents);
}
