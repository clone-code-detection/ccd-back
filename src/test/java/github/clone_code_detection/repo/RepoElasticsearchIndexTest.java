package github.clone_code_detection.repo;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RepoElasticsearchIndexTest {
    @Test
    void testConvert() {
        CreateIndexRequest request = new CreateIndexRequest("")
                .source("""
                        {
                               "settings": {
                                 "analysis": {
                                   "analyzer": {
                                     "source_code_analyzer": {
                                       "filter": [
                                         "lowercase"
                                       ],
                                       "tokenizer": "source_code_tokenizer"
                                     },
                                     "source_code_normalized_analyzer": {
                                       "filter": [
                                         "lowercase"
                                       ],
                                       "tokenizer": "source_code_normalized_tokenizer"
                                     }
                                   },
                                   "tokenizer": {
                                     "source_code_tokenizer": {
                                       "type": "java_experiment",
                                       "version": 1
                                     },
                                     "source_code_normalized_tokenizer": {
                                       "type": "java_experiment",
                                       "version": 2
                                     }
                                   }
                                 }
                               },
                               "mappings": {
                                 "properties": {
                                   "meta": {
                                     "type": "object",
                                     "dynamic": "true"
                                   },
                                   "source_code": {
                                     "type": "text",
                                     "analyzer": "source_code_analyzer"
                                   },
                                   "source_code_normalized": {
                                     "type": "text",
                                     "analyzer": "source_code_normalized_analyzer"
                                   }
                                 }
                               }
                             }
                        }""", XContentType.JSON);
    }
}