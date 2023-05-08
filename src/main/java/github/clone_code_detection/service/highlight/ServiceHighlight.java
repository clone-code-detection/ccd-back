package github.clone_code_detection.service.highlight;

import github.clone_code_detection.entity.authenication.UserImpl;
import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.entity.highlight.document.HighlightSessionDocument;
import github.clone_code_detection.entity.highlight.document.HighlightSessionStatus;
import github.clone_code_detection.entity.highlight.document.HighlightSingleDocument;
import github.clone_code_detection.entity.highlight.document.HighlightSingleTargetMatchDocument;
import github.clone_code_detection.entity.highlight.report.HighlightSessionDetailDTO;
import github.clone_code_detection.entity.highlight.report.HighlightSessionReportDTO;
import github.clone_code_detection.entity.highlight.report.HighlightSingleSourceDTO;
import github.clone_code_detection.entity.highlight.report.HighlightWordMatchDTO;
import github.clone_code_detection.entity.highlight.request.HighlightSessionRequest;
import github.clone_code_detection.entity.index.IndexInstruction;
import github.clone_code_detection.entity.query.QueryInstruction;
import github.clone_code_detection.exceptions.highlight.ElasticsearchQueryException;
import github.clone_code_detection.exceptions.highlight.ResourceNotFoundException;
import github.clone_code_detection.repo.RepoElasticsearchQuery;
import github.clone_code_detection.repo.RepoFileDocument;
import github.clone_code_detection.repo.RepoHighlightSessionDocument;
import github.clone_code_detection.repo.RepoHighlightSingleMatchDocument;
import github.clone_code_detection.service.index.ServiceIndex;
import github.clone_code_detection.util.FileSystemUtil;
import github.clone_code_detection.util.LanguageUtil;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.core.MultiTermVectorsResponse;
import org.elasticsearch.client.core.TermVectorsResponse;
import org.elasticsearch.common.util.set.Sets;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

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
    private final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

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

    /**
     * Get user from SecurityContextHolder
     */
    @Nullable
    public static UserImpl getUserFromContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getPrincipal() instanceof UserImpl userImpl) return userImpl;
        return null;
    }

    private static List<HighlightWordMatchDTO> extractTermVectorsResponse(MultiTermVectorsResponse response) {
        List<HighlightWordMatchDTO> res = new ArrayList<>();
        assert response.getTermVectorsResponses().size() == 2;

        TermVectorsResponse source = response.getTermVectorsResponses().get(0);
        TermVectorsResponse target = response.getTermVectorsResponses().get(1);
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
        TermVectorsResponse.TermVector termVector = getTermVectorByFieldName(termVectorsResponse.getTermVectorsList(), SOURCE_CODE_FIELD);
        if (termVector == null)
            return res;
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
        return res;
    }

    private static TermVectorsResponse.TermVector getTermVectorByFieldName(List<TermVectorsResponse.TermVector> termVectors, String fieldName) {
        for (TermVectorsResponse.TermVector termVector : termVectors) {
            if (termVector.getFieldName().equals(fieldName))
                return termVector;
        }
        return null;
    }

    @Transactional
    public HighlightSingleSourceDTO getSingleSourceMatchById(String uuid) {
        HighlightSingleDocument singleDocument = repoHighlightSingleMatchDocument.findById(
                        UUID.fromString(uuid))
                .orElseThrow();
        return HighlightSingleSourceDTO.from(singleDocument, this::getHighlightWordMatchDTOS);
    }

    @NonNull
    private List<HighlightWordMatchDTO> getHighlightWordMatchDTOS(HighlightSingleTargetMatchDocument singleDocument) {
        FileDocument source = singleDocument.getSource()
                .getSource();
        FileDocument target = singleDocument.getTarget();
        MultiTermVectorsResponse multiTermVectors = repoElasticsearchQuery.getMultiTermVectors(
                source, target);
        return extractTermVectorsResponse(multiTermVectors);
    }

    @Transactional
    public HighlightSessionDetailDTO getHighlightSessionById(String uuid) {
        Optional<HighlightSessionDocument> sessionDocumentById = repoHighlightSessionDocument.findById(
                UUID.fromString(uuid));
        HighlightSessionDocument resource = sessionDocumentById.orElseThrow(
                () -> new ResourceNotFoundException("Resource with uuid not found"));
        return HighlightSessionDetailDTO.from(resource);
    }

    @Transactional
    public HighlightSessionDetailDTO highlight(@NotNull MultipartFile source, @Nonnull IndexInstruction instruction) {
        // Validate and extract file from source
        FileSystemUtil.validate(source);
        Collection<FileDocument> sourceDocuments = FileSystemUtil.extractDocuments(source);
        // Highlight source documents
        HighlightSessionDocument.HighlightSessionDocumentBuilder sessionBuilder = HighlightSessionDocument.builder();
        List<HighlightSingleDocument> hits = new ArrayList<>();
        for (FileDocument sourceDocument : sourceDocuments) {
            // for each document, get highlight request
            HighlightSingleDocument highlightSingleDocument = extractSingleDocument(sourceDocument);
            hits.add(highlightSingleDocument);
        }
        // Build highlight session
        sessionBuilder.matches(hits);
        HighlightSessionDocument highlightSessionDocument = sessionBuilder.build();
        highlightSessionDocument.setUser(getUserFromContext());
        highlightSessionDocument.setName(FileSystemUtil.getFileName(source));
        highlightSessionDocument.setMainLanguage(LanguageUtil.getMainLanguageOfSingleDocuments(hits));
        highlightSessionDocument = repoHighlightSessionDocument.save(highlightSessionDocument);

        // Save source files before indexing
        sourceDocuments = repoFileDocument.saveAll(sourceDocuments);
        // Index the file into es
        instruction.setFiles(sourceDocuments);
        serviceIndex.indexAllDocuments(instruction);

        return HighlightSessionDetailDTO.from(highlightSessionDocument);
    }

    @Transactional
    public Collection<HighlightSessionDocument.HighlightSessionProjection> getAllSession() {
        UserImpl principal = getUserFromContext();
        assert principal != null;
        return repoHighlightSessionDocument.getAllByUserId(principal.getId());
    }

    /**
     * For each file, query with highlight enabled
     */
    private HighlightSingleDocument extractSingleDocument(FileDocument source) {
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
    private HighlightSingleDocument parseResponse(FileDocument source, SearchResponse search) {
        HighlightSingleDocument.HighlightSingleDocumentBuilder builder = HighlightSingleDocument.builder();
        // get hits
        Collection<HighlightSingleTargetMatchDocument> matches = new ArrayList<>();
        HighlightSingleDocument highlightSingleDocument = builder.source(source)
                .matches(matches)
                .build();
        for (SearchHit hit : search.getHits()) {
            String id = hit.getId();
            Optional<FileDocument> fileDocument = repoFileDocument.findById(UUID.fromString(id));
            if (fileDocument.isEmpty()) continue;
            HighlightSingleTargetMatchDocument singleMatch = HighlightSingleTargetMatchDocument.builder()
                    .score(hit.getScore())
                    .source(highlightSingleDocument)
                    .target(fileDocument.get())
                    .build();
            matches.add(singleMatch);
        }
        return highlightSingleDocument;
    }

    public HighlightSessionReportDTO createHighlightSession(HighlightSessionRequest request, IndexInstruction instruction) {
        // Create new empty highlight session
        HighlightSessionDocument.HighlightSessionDocumentBuilder sessionBuilder = HighlightSessionDocument.builder();
        HighlightSessionDocument highlightSessionDocument = sessionBuilder.build();
        highlightSessionDocument.setUser(getUserFromContext());
        highlightSessionDocument.setName(request.getFileName());
        highlightSessionDocument = repoHighlightSessionDocument.save(highlightSessionDocument);
        // Assign session id of empty highlight session into each source document
        Collection<FileDocument> sourceDocuments = request.getSources();
        // Save file to with session id to get file id
        instruction.setFiles(repoFileDocument.saveAll(sourceDocuments));
        executor.execute(new HighlightProcessor(highlightSessionDocument, instruction));
        return HighlightSessionReportDTO.builder()
                .sessionId(highlightSessionDocument.getId())
                .sessionName(highlightSessionDocument.getName())
                .build();
    }

    @Data
    private class HighlightProcessor implements Runnable {
        private HighlightSessionDocument session;
        private IndexInstruction instruction;

        public HighlightProcessor(HighlightSessionDocument session, IndexInstruction instruction) {
            this.session = session;
            this.instruction = instruction;
        }

        @Override
        public void run() {
            List<HighlightSingleDocument> hits = new ArrayList<>();
            for (FileDocument sourceDocument : instruction.getFiles()) {
                // for each document, get highlight request
                HighlightSingleDocument highlightSingleDocument = extractSingleDocument(sourceDocument);
                hits.add(highlightSingleDocument);
            }
            session.setMatches(hits);
            session.setMainLanguage(LanguageUtil.getMainLanguageOfSingleDocuments(hits));
            session.setStatus(HighlightSessionStatus.DONE);
            repoHighlightSessionDocument.save(session);
            serviceIndex.indexAllDocuments(instruction);
        }
    }
}