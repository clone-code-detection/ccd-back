package github.clone_code_detection.service.highlight;

import github.clone_code_detection.entity.authenication.UserImpl;
import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.entity.highlight.OffsetResponse;
import github.clone_code_detection.entity.highlight.document.HighlightMatchDocument;
import github.clone_code_detection.entity.highlight.document.HighlightSessionDocument;
import github.clone_code_detection.entity.highlight.document.HighlightSingleDocument;
import github.clone_code_detection.entity.highlight.report.HighlightSessionReportDTO;
import github.clone_code_detection.entity.highlight.report.HighlightSingleMatchDTO;
import github.clone_code_detection.entity.index.IndexInstruction;
import github.clone_code_detection.entity.query.QueryInstruction;
import github.clone_code_detection.exceptions.highlight.ElasticsearchQueryException;
import github.clone_code_detection.repo.RepoElasticsearchQuery;
import github.clone_code_detection.repo.RepoFileDocument;
import github.clone_code_detection.repo.RepoHighlightSessionDocument;
import github.clone_code_detection.repo.RepoHighlightSingleMatchDocument;
import github.clone_code_detection.service.index.ServiceIndex;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
public class ServiceHighlight {
    private final ServiceIndex serviceIndex;
    private final RepoElasticsearchQuery repoElasticsearchQuery;
    private final RepoHighlightSessionDocument repoHighlightSessionDocument;
    private final RepoHighlightSingleMatchDocument repoHighlightSingleMatchDocument;
    private final RepoFileDocument repoFileDocument;

    @Autowired
    public ServiceHighlight(ServiceIndex serviceIndex,
                            RepoElasticsearchQuery repoElasticsearchQuery,
                            RepoHighlightSessionDocument repoHighlightSessionDocument,
                            RepoHighlightSingleMatchDocument repoHighlightSingleMatchDocument,
                            RepoFileDocument repoFileDocument) {
        this.serviceIndex = serviceIndex;
        this.repoElasticsearchQuery = repoElasticsearchQuery;
        this.repoHighlightSessionDocument = repoHighlightSessionDocument;
        this.repoHighlightSingleMatchDocument = repoHighlightSingleMatchDocument;
        this.repoFileDocument = repoFileDocument;
    }

    @Transactional
    public HighlightSingleMatchDTO getSingleMatchById(String uuid) {
        HighlightSingleDocument singleDocument = repoHighlightSingleMatchDocument.findById(
                                                                                         UUID.fromString(uuid))
                                                                                 .orElseThrow();
        return HighlightSingleMatchDTO.fromHighlightSingleMatchDTO(singleDocument);
    }

    @Transactional
    public HighlightSessionReportDTO highlight(@NotNull MultipartFile source, @Nonnull IndexInstruction sourceIndexInstruction) {
        Collection<FileDocument> sourceDocuments = serviceIndex.indexAllDocuments(source, sourceIndexInstruction);
        HighlightSessionDocument.HighlightSessionDocumentBuilder sessionBuilder = HighlightSessionDocument.builder();
        List<HighlightSingleDocument> hits = new ArrayList<>();
        for (FileDocument sourceDocument : sourceDocuments) {
            // for each document, get highlight request
            List<HighlightSingleDocument> highlightSingleDocument = extractSingleDocument(sourceDocument);
            hits.addAll(highlightSingleDocument);
        }
        sessionBuilder.matches(hits);
        HighlightSessionDocument highlightSessionDocument = sessionBuilder.build();
        log.info("[Highlight service] highlight report: {}", highlightSessionDocument);
        highlightSessionDocument.setUser(getUserFromContext());
        repoHighlightSessionDocument.save(highlightSessionDocument);
        return HighlightSessionReportDTO.from(highlightSessionDocument);
    }

    @Transactional
    public Collection<HighlightSessionDocument.HighlightSessionProjection> getAllSession() {
        UserImpl principal = getUserFromContext();
        return repoHighlightSessionDocument.getAllByUser_Id(principal.getId());
    }

    /**
     * For each file, query with highlight enabled
     */
    private List<HighlightSingleDocument> extractSingleDocument(FileDocument source) {
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
        return parseResponse(source, searchResponse);
    }

    /**
     * Extract match fields from es search response
     */
    private List<HighlightSingleDocument> parseResponse(FileDocument source, SearchResponse search) {
        List<HighlightSingleDocument> res = new ArrayList<>();
        // get hits
        // TODO (bug)
        List<FileDocument> all = repoFileDocument.findAll();
        for (SearchHit hit : search.getHits()) {
            String id = hit.getId();
            log.info("Match id: {}", id);
            Optional<FileDocument> fileDocument = repoFileDocument.findById(UUID.fromString(id));
            if (fileDocument.isEmpty()) continue;
            OffsetResponse offsetResponse = extractOffsetFromHit(hit);
            Collection<HighlightMatchDocument> matches = offsetResponse.getMatches()
                                                                       .stream()
                                                                       .map(integerIntegerPair -> HighlightMatchDocument.builder()
                                                                                                                        .start(integerIntegerPair.getFirst())
                                                                                                                        .end(integerIntegerPair.getSecond())
                                                                                                                        .build())
                                                                       .toList();
            res.add(HighlightSingleDocument.builder()
                                           .source(source)
                                           .target(fileDocument.get())
                                           .matches(matches)
                                           .build());
        }
        return res;
    }

    /**
     * Search hit is in special format with search-highlight feature of elasticsearch
     */
    private static OffsetResponse extractOffsetFromHit(SearchHit hit) {
        Map<String, HighlightField> highlightFields = hit.getHighlightFields();
        // traverse highlight fields
        Map<String, OffsetResponse> offsetResponseMap = new HashMap<>();
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
        return offsetResponseMap.get("source_code");
    }

    /**
     * Get user from SecurityContextHolder
     */
    private static UserImpl getUserFromContext() {
        Authentication authentication = SecurityContextHolder.getContext()
                                                             .getAuthentication();
        return (UserImpl) authentication.getPrincipal();
    }
}