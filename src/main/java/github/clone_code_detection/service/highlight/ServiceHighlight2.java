package github.clone_code_detection.service.highlight;

import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.entity.highlight.OffsetResponse;
import github.clone_code_detection.entity.highlight.report.HighlightMatch;
import github.clone_code_detection.entity.highlight.report.HighlightSessionDocument;
import github.clone_code_detection.entity.highlight.report.HighlightSessionReportDTO;
import github.clone_code_detection.entity.highlight.report.HighlightSingleDocument;
import github.clone_code_detection.entity.index.IndexInstruction;
import github.clone_code_detection.entity.query.QueryInstruction;
import github.clone_code_detection.exceptions.highlight.ElasticsearchQueryException;
import github.clone_code_detection.repo.RepoElasticsearchQuery;
import github.clone_code_detection.repo.RepoHighlightSessionDocument;
import github.clone_code_detection.service.index.ServiceIndex;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;

@Service
@Validated
@Slf4j
@Transactional
public class ServiceHighlight2 {
    private final ServiceIndex serviceIndex;
    private final RepoElasticsearchQuery repoElasticsearchQuery;
    private final RepoHighlightSessionDocument repoHighlightSessionDocument;

    @Autowired
    public ServiceHighlight2(ServiceIndex serviceIndex, RepoElasticsearchQuery repoElasticsearchQuery, RepoHighlightSessionDocument repoHighlightSessionDocument) {
        this.serviceIndex = serviceIndex;
        this.repoElasticsearchQuery = repoElasticsearchQuery;
        this.repoHighlightSessionDocument = repoHighlightSessionDocument;
    }

    @Transactional
    public HighlightSessionReportDTO highlight(@NotNull MultipartFile source, @Nonnull IndexInstruction sourceIndexInstruction, @NotNull MultipartFile target, @NotNull IndexInstruction targetIndexInstruction) {
        Collection<FileDocument> sourceDocuments = serviceIndex.indexAllDocuments(source, sourceIndexInstruction);
        HighlightSessionDocument.HighlightSessionDocumentBuilder sessionBuilder = HighlightSessionDocument.builder();
        for (FileDocument sourceDocument : sourceDocuments) {
            // index
            Collection<FileDocument> targetDocuments = serviceIndex.indexAllDocuments(target, targetIndexInstruction);
            List<HighlightSingleDocument> hits = new ArrayList<>();
            // for each document, get highlight request
            for (FileDocument document : targetDocuments) {
                HighlightSingleDocument highlightSingleDocument = extractSingleDocument(sourceDocument, document);
                hits.add(highlightSingleDocument);
            }
            sessionBuilder.matches(hits);
        }
        HighlightSessionDocument highlightSessionDocument = sessionBuilder.build();
        log.info("[Highlight service] highlight report: {}", highlightSessionDocument);
        repoHighlightSessionDocument.save(highlightSessionDocument);
        return HighlightSessionReportDTO.from(highlightSessionDocument);
    }

    private HighlightSingleDocument extractSingleDocument(FileDocument source, FileDocument target) {
        HighlightSingleDocument.HighlightSingleDocumentBuilder singleDocumentBuilder;
        List<HighlightMatch> matches = new ArrayList<>();
        singleDocumentBuilder = HighlightSingleDocument.builder()
                                                       .source(source)
                                                       .target(target);

        QueryInstruction queryInstruction = QueryInstruction.builder()
                                                            .queryDocument(source)
                                                            .includeHighlight(true)
                                                            .minimumShouldMatch("80%")
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

        // add offsets in match
        for (Pair<Integer, Integer> pair : offsetResponse.getMatches()) {
            matches.add(HighlightMatch.builder()
                                      .start(pair.getFirst())
                                      .end(pair.getSecond())
                                      .build());
        }
        singleDocumentBuilder.matches(matches);


        return singleDocumentBuilder.build();
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

//    /**
//     * Get file content from database
//     */
//    private String retrieveFileContent(String id) {
//        UUID uuid = UUID.fromString(id);
//        Optional<FileDocument> savedFile = repoFileDocument.findById(uuid);
//        FileDocument fileDocument = savedFile.orElseThrow();
//        return new String(fileDocument.getContent(), StandardCharsets.UTF_8);
//    }
}