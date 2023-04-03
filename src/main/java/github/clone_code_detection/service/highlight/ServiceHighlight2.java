package github.clone_code_detection.service.highlight;

import github.clone_code_detection.entity.FileDocument;
import github.clone_code_detection.entity.highlight.OffsetResponse;
import github.clone_code_detection.entity.index.IndexInstruction;
import github.clone_code_detection.entity.query.QueryInstruction;
import github.clone_code_detection.exceptions.highlight.ElasticsearchQueryException;
import github.clone_code_detection.repo.RepoElasticsearchQuery;
import github.clone_code_detection.service.index.ServiceIndex;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Validated
@Slf4j
public class ServiceHighlight2 {
    private final ServiceIndex serviceIndex;
    private final RepoElasticsearchQuery repoElasticsearchQuery;

    @Autowired
    public ServiceHighlight2(ServiceIndex serviceIndex, RepoElasticsearchQuery repoElasticsearchQuery) {
        this.serviceIndex = serviceIndex;
        this.repoElasticsearchQuery = repoElasticsearchQuery;
    }

    @Transactional
    public void test(@Nonnull MultipartFile file, @NotNull IndexInstruction indexInstruction) {
        // index
        Collection<FileDocument> documents = serviceIndex.indexAllDocuments(file, indexInstruction);
        // for each document, get highlight request
        for (FileDocument document : documents) {
            QueryInstruction queryInstruction = QueryInstruction.builder()
                                                                .build();
            SearchResponse searchResponse;
            try {
                searchResponse = repoElasticsearchQuery.query(queryInstruction);
            } catch (IOException e) {
                log.error("Error querying elasticsearch", e);
                throw new ElasticsearchQueryException("[Service highlight] Failed to query es");
            }
            // parse response
            Map<String, OffsetResponse> offsetResponseMap = parseResponse(searchResponse);
            OffsetResponse offsetResponse = offsetResponseMap.get("source_code");
        }
    }

    /**
     * Extract match fields from es search response
     */
    private Map<String, OffsetResponse> parseResponse(SearchResponse search) {
        Map<String, OffsetResponse> offsetResponseMap = new HashMap<>();

        // get hits
        for (SearchHit hit : search.getHits()) {
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            // traverse highlight fields
            for (Map.Entry<String, HighlightField> entry : highlightFields.entrySet()) {
                String fieldName = entry.getKey();
                HighlightField highlightField = entry.getValue();
                OffsetResponse offsetResponse = null;
                // extract info and concat all fragments
                for (Text fragment : highlightField.fragments()) {
                    OffsetResponse framgentOffsetResponse = OffsetResponse.fromString(fragment.string());
                    if (offsetResponse == null) offsetResponse = framgentOffsetResponse;
                    offsetResponse = offsetResponse.union(framgentOffsetResponse);
                }
                offsetResponseMap.put(fieldName, offsetResponse);
            }
        }
        return offsetResponseMap;
    }

    /**
     * Get file content from database
     */
    private String retrieveFileContent(String id) {
        UUID uuid = UUID.fromString(id);
        Optional<FileDocument> savedFile = repoFileDocument.findById(uuid);
        FileDocument fileDocument = savedFile.orElseThrow();
        return new String(fileDocument.getContent(), StandardCharsets.UTF_8);
    }
}