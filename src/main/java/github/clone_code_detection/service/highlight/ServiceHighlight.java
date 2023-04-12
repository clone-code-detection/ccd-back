package github.clone_code_detection.service.highlight;

import github.clone_code_detection.entity.authenication.UserImpl;
import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.entity.highlight.OffsetResponse;
import github.clone_code_detection.entity.highlight.document.HighlightMatchDocument;
import github.clone_code_detection.entity.highlight.document.HighlightSessionDocument;
import github.clone_code_detection.entity.highlight.document.HighlightSingleDocument;
import github.clone_code_detection.entity.highlight.report.HighlightSessionReportDTO;
import github.clone_code_detection.entity.highlight.report.HighlightSingleMatchDTO;
import github.clone_code_detection.entity.highlight.report.HighlightWordMatchDTO;
import github.clone_code_detection.entity.index.IndexInstruction;
import github.clone_code_detection.entity.query.QueryInstruction;
import github.clone_code_detection.exceptions.highlight.ElasticsearchQueryException;
import github.clone_code_detection.repo.RepoElasticsearchQuery;
import github.clone_code_detection.repo.RepoFileDocument;
import github.clone_code_detection.repo.RepoHighlightSessionDocument;
import github.clone_code_detection.repo.RepoHighlightSingleMatchDocument;
import github.clone_code_detection.service.index.ServiceIndex;
import github.clone_code_detection.util.FileSystemUtil;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.core.MultiTermVectorsResponse;
import org.elasticsearch.client.core.TermVectorsResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.util.set.Sets;
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

import static github.clone_code_detection.repo.RepoElasticsearchQuery.SOURCE_CODE_FIELD;

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

    @Deprecated
    @Transactional
    public HighlightSingleMatchDTO getSingleMatchById(String uuid) {
        HighlightSingleDocument singleDocument = repoHighlightSingleMatchDocument.findById(
                                                                                         UUID.fromString(uuid))
                                                                                 .orElseThrow();
        return HighlightSingleMatchDTO.fromHighlightSingleMatchDTO(singleDocument);
    }

    @Transactional
    public HighlightSingleMatchDTO getSingleMatchByIdImproved(String uuid) {
        HighlightSingleDocument singleDocument = repoHighlightSingleMatchDocument.findById(
                                                                                         UUID.fromString(uuid))
                                                                                 .orElseThrow();
        FileDocument source = singleDocument.getSource();
        FileDocument target = singleDocument.getTarget();
        MultiTermVectorsResponse multiTermVectors = repoElasticsearchQuery.getMultiTermVectors(
                source, target);
        List<HighlightWordMatchDTO> highlightWordMatchDTOS = extractTermVectorsResponse(multiTermVectors);
        return HighlightSingleMatchDTO.builder()
                                      .source(source.getContentAsString())
                                      .target(target.getContentAsString())
                                      .matches(highlightWordMatchDTOS)
                                      .build();
    }

    @Transactional
    public HighlightSessionReportDTO highlight(@NotNull MultipartFile source, @Nonnull IndexInstruction sourceIndexInstruction) {
//        Collection<FileDocument> sourceDocuments = serviceIndex.indexAllDocuments(source, sourceIndexInstruction);
        Collection<FileDocument> sourceDocuments = FileSystemUtil.extractDocuments(source);
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

    private static List<HighlightWordMatchDTO> extractTermVectorsResponse(MultiTermVectorsResponse response) {
        List<HighlightWordMatchDTO> res = new ArrayList<>();
        assert response.getTermVectorsResponses()
                       .size() == 2;

        TermVectorsResponse source = response.getTermVectorsResponses()
                                             .get(0);
        TermVectorsResponse target = response.getTermVectorsResponses()
                                             .get(1);
        // traverse every document in query
        Map<String, List<Integer[]>> sourceMap = extractMatches(source);
        Map<String, List<Integer[]>> targetMap = extractMatches(target);
        Set<String> commonKey = Sets.intersection(sourceMap.keySet(), targetMap.keySet());
        for (String common : commonKey) {
            HighlightWordMatchDTO wordMatchDTO = HighlightWordMatchDTO.builder()
                                                                      .word(common)
                                                                      .sourceMatches(sourceMap.get(common))
                                                                      .targetMatches(targetMap.get(common))
                                                                      .build();
            res.add(wordMatchDTO);
        }
        return res;
    }

    private static Map<String, List<Integer[]>> extractMatches(TermVectorsResponse termVectorsResponse) {
        Map<String, List<Integer[]>> res = new HashMap<>();
        for (TermVectorsResponse.TermVector termVector : termVectorsResponse.getTermVectorsList()) {
            if (!termVector.getFieldName()
                           .equals(SOURCE_CODE_FIELD)) continue;
            for (TermVectorsResponse.TermVector.Term term : termVector.getTerms()) {
                String termValue = term.getTerm();
                var tokens = term.getTokens();
                List<Integer[]> ls = new ArrayList<>();
                for (TermVectorsResponse.TermVector.Token token : tokens) {
                    Integer startOffset = token.getStartOffset();
                    Integer endOffset = token.getEndOffset();
                    ls.add(new Integer[]{startOffset, endOffset});
                }
                res.put(termValue, ls);
            }
            break;
        }
        return res;
    }
}