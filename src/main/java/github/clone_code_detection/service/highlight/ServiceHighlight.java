package github.clone_code_detection.service.highlight;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import github.clone_code_detection.exceptions.highlight.HighlightSessionException;
import github.clone_code_detection.exceptions.highlight.ResourceNotFoundException;
import github.clone_code_detection.repo.*;
import github.clone_code_detection.service.index.ServiceIndex;
import github.clone_code_detection.util.FileSystemUtil;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.core.MultiTermVectorsResponse;
import org.elasticsearch.client.core.TermVectorsResponse;
import org.elasticsearch.common.util.set.Sets;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.lang.NonNull;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final RepoHighlightSingleTargetMatchDocument repoHighlightSingleTargetMatchDocument;
    private final RepoFileDocument repoFileDocument;
    private final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

    @Autowired
    public ServiceHighlight(ServiceIndex serviceIndex, RepoElasticsearchQuery repoElasticsearchQuery, RepoHighlightSessionDocument repoHighlightSessionDocument, RepoHighlightSingleMatchDocument repoHighlightSingleMatchDocument, RepoHighlightSingleTargetMatchDocument repoHighlightSingleTargetMatchDocument, RepoFileDocument repoFileDocument) {
        this.serviceIndex = serviceIndex;
        this.repoElasticsearchQuery = repoElasticsearchQuery;
        this.repoHighlightSessionDocument = repoHighlightSessionDocument;
        this.repoHighlightSingleMatchDocument = repoHighlightSingleMatchDocument;
        this.repoHighlightSingleTargetMatchDocument = repoHighlightSingleTargetMatchDocument;
        this.repoFileDocument = repoFileDocument;
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

    /**
     * Get user from SecurityContextHolder
     */
    @Nullable
    public static UserImpl getUserFromContext() {
        Authentication authentication = SecurityContextHolder.getContext()
                                                             .getAuthentication();
        if (authentication.getPrincipal() instanceof UserImpl userImpl) return userImpl;
        return null;
    }

    private static Map<String, List<Integer[]>> extractMatches(TermVectorsResponse termVectorsResponse) {
        Map<String, List<Integer[]>> res = new HashMap<>();
        TermVectorsResponse.TermVector termVector = getTermVectorByFieldName(termVectorsResponse.getTermVectorsList(),
                SOURCE_CODE_FIELD);
        if (termVector == null) return res;
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
            if (termVector.getFieldName()
                          .equals(fieldName)) return termVector;
        }
        return null;
    }

    @Transactional
    public HighlightSessionReportDTO createHighlightSession(HighlightSessionRequest request, IndexInstruction instruction) {
        // Create new empty highlight session
        HighlightSessionDocument.HighlightSessionDocumentBuilder sessionBuilder = HighlightSessionDocument.builder();
        HighlightSessionDocument highlightSessionDocument = sessionBuilder.build();
        highlightSessionDocument.setUser(getUserFromContext());
        highlightSessionDocument.setName(request.getSessionName());
        highlightSessionDocument = repoHighlightSessionDocument.save(highlightSessionDocument);
        // Assign session id of empty highlight session into each source document
        Collection<FileDocument> sourceDocuments = request.getSources();
        // Save file linking id of session
        final UUID sessionId = highlightSessionDocument.getId();
        sourceDocuments.forEach(sourceDocument -> {
            sourceDocument.setSessionId(sessionId);
            sourceDocument.setUser(getUserFromContext());
        });
        instruction.setFiles(repoFileDocument.saveAll(sourceDocuments));
        executor.execute(new HighlightProcessor(highlightSessionDocument, instruction));
        return HighlightSessionReportDTO.builder()
                                        .sessionId(highlightSessionDocument.getId())
                                        .sessionName(highlightSessionDocument.getName())
                                        .build();
    }

    public Collection<HighlightReturn> handleAdvancedHighlightById(String id) {
        if (id.equals("undefined")) return new ArrayList<>();
        HighlightSingleTargetMatchDocument singleDocument = repoHighlightSingleTargetMatchDocument.findById(
                                                                                                          UUID.fromString(id))
                                                                                                  .orElseThrow();
        return handleAdvancedHighlight(singleDocument);
    }

    /**
     * @return Map with token as string, and SORTED LIST of position as value
     */
    public Map<String, List<Integer>> mapTokensByValue(@NonNull TermVectorsResponse termVectorsResponse) {
        TermVectorsResponse.TermVector termVector = getTermVectorByFieldName(termVectorsResponse.getTermVectorsList(),
                SOURCE_CODE_FIELD);
        if (termVector == null) throw new RuntimeException();
        Map<String, List<Integer>> res = new HashMap<>();
        for (TermVectorsResponse.TermVector.Term term : termVector.getTerms()) {
            String termValue = term.getTerm();
            res.computeIfAbsent(termValue, value -> new ArrayList<>());
            var list = res.get(termValue);
            for (TermVectorsResponse.TermVector.Token token : term.getTokens()) list.add(token.getPosition());
            list.sort(Integer::compareTo);
        }
        return res;
    }

    // PriorityQueue is ordered by term length
    public ArrayList<PriorityQueue<TokenWrapper>> mapTokensByPosition(@NonNull TermVectorsResponse termVectorsResponse) {
        TermVectorsResponse.TermVector termVector = getTermVectorByFieldName(termVectorsResponse.getTermVectorsList(),
                SOURCE_CODE_FIELD);
        if (termVector == null) throw new RuntimeException();
        int size = termVector.getTerms()
                             .stream()
                             .flatMap(term -> term.getTokens()
                                                  .stream())
                             .map(TermVectorsResponse.TermVector.Token::getPosition)
                             .max(Comparator.naturalOrder())
                             .orElseThrow();
        ArrayList<PriorityQueue<TokenWrapper>> res = new ArrayList<>();
        for (int i = 0; i <= size; i++)
            res.add(new PriorityQueue<>(Comparator.comparing(TokenWrapper::getLength)
                                                  .reversed()));

        for (TermVectorsResponse.TermVector.Term term : termVector.getTerms()) {
            var tokenValue = term.getTerm();
            for (TermVectorsResponse.TermVector.Token token : term.getTokens()) {
                TokenWrapper tokenWrapper = TokenWrapper.fromToken(tokenValue, token);
                Integer position = tokenWrapper.getPosition();
                res.get(position)
                   .add(tokenWrapper);
            }
        }
        return res;
    }

    /**
     * @return list of extract return
     * @implNote: requires field mapping to have term vector position offset
     */
    public Collection<HighlightReturn> handleAdvancedHighlight(HighlightSingleTargetMatchDocument targetMatchDocument) {
        var source = targetMatchDocument.getSource()
                                        .getSource();
        var target = targetMatchDocument.getTarget();
        MultiTermVectorsResponse multiTermVectors = repoElasticsearchQuery.getMultiTermVectors(source, target);
        List<TermVectorsResponse> termVectorsResponses = multiTermVectors.getTermVectorsResponses();
        assert termVectorsResponses.size() == 2;
        var sourceTermVectorResponse = termVectorsResponses.get(0);
        var targetTermVectorResponse = termVectorsResponses.get(1);

        return getExtractReturnCollection(sourceTermVectorResponse, targetTermVectorResponse);
    }

    public Collection<HighlightReturn> getExtractReturnCollection(TermVectorsResponse source, TermVectorsResponse target) {
        // map source tokens by position
        ArrayList<PriorityQueue<TokenWrapper>> sourceMapByPosition = mapTokensByPosition(source);
        // map tokens by position
        ArrayList<PriorityQueue<TokenWrapper>> targetMapByPosition = mapTokensByPosition(target);
        // map tokens by value
        Map<String, List<Integer>> targetByValue = mapTokensByValue(target);

        // logic
        Collection<HighlightReturn> res = new ArrayList<>();
        int i = 0;
        while (i < sourceMapByPosition.size()) {
            HighlightReturn extracted = extracted(sourceMapByPosition, targetMapByPosition, targetByValue, i);
            // If the block has no matching, increase token by one
            i += extracted.longestCommonLength > 0 ? extracted.longestCommonLength : 1;
            res.add(extracted);
        }
        return res;
    }

    // Sorted lists
    private static <T extends Comparable<T>> boolean containsAny(List<T> a, List<T> b) {
        for (T t : b) {
            if (Collections.binarySearch(a, t) >= 0) return true;
        }
        return false;
    }

    private boolean condition(Collection<TokenWrapper> target, Collection<TokenWrapper> source) {
        return containsAny(target.stream()
                                 .map(TokenWrapper::getToken)
                                 .sorted()
                                 .collect(Collectors.toList()),
                source.stream()
                      .map(TokenWrapper::getToken)
                      .sorted()
                      .collect(Collectors.toList()));
    }

    private HighlightReturn extracted(ArrayList<PriorityQueue<TokenWrapper>> sourceMapByPosition,
                                      ArrayList<PriorityQueue<TokenWrapper>> targetMapByPosition,
                                      Map<String, List<Integer>> targetByValue,
                                      int i) {
        Set<Integer> targetStartingPositions = getTargetStartingPositions(sourceMapByPosition, targetByValue, i);
        if (targetStartingPositions.isEmpty()) {
            var sourceBlock = extractBlockFromPosition(i, i + 1, sourceMapByPosition);
            return new HighlightReturn(0).sourceBlock(sourceBlock);
        }

        int sourceSize = sourceMapByPosition.size();
        int targetSize = targetMapByPosition.size();
        List<Pair<Integer, Integer>> pairs = new ArrayList<>();
        int maxCommonLength = 1;

        for (Integer targetStartingPosition : targetStartingPositions) {
            int commonLength = 0;
            int sourceStartingPosition = i;
            int targetEndingPosition = targetStartingPosition;
            // legal
            var sourceSynonyms = sourceMapByPosition.get(sourceStartingPosition);
            var targetSynonyms = targetMapByPosition.get(targetStartingPosition);

            while (condition(targetSynonyms, sourceSynonyms)) {
                sourceStartingPosition++;
                targetEndingPosition++;
                commonLength++;

                if (sourceStartingPosition >= sourceSize) break;
                if (targetEndingPosition >= targetSize) break;

                sourceSynonyms = sourceMapByPosition.get(sourceStartingPosition);
                targetSynonyms = targetMapByPosition.get(targetEndingPosition);
            }
            if (commonLength > maxCommonLength) maxCommonLength = commonLength;
            pairs.add(Pair.of(targetStartingPosition, targetEndingPosition));
        }
        HighlightReturn res = new HighlightReturn(maxCommonLength);
        for (Pair<Integer, Integer> tagetMatchPair : pairs) {
            int length = tagetMatchPair.getSecond() - tagetMatchPair.getFirst();
            if (maxCommonLength == length) {
                var matchBlock = extractBlockFromPosition(tagetMatchPair.getFirst(), tagetMatchPair.getSecond(),
                        targetMapByPosition);
                res.targetBlock(matchBlock);
            }
        }
        var sourceMatchBlock = extractBlockFromPosition(i, maxCommonLength + i, sourceMapByPosition);
        res.sourceBlock(sourceMatchBlock);
        return res;
    }

    private static Set<Integer> getTargetStartingPositions(ArrayList<PriorityQueue<TokenWrapper>> sourceMapByPosition,
                                                           Map<String, List<Integer>> targetByValue,
                                                           int i) {
        PriorityQueue<TokenWrapper> synonyms = sourceMapByPosition.get(i);
        return synonyms.stream()
                       .map(TokenWrapper::getToken)
                       .flatMap(tokenValue -> {
                           if (!targetByValue.containsKey(tokenValue)) return Stream.empty();
                           return targetByValue.get(tokenValue)
                                               .stream();
                       })
                       .collect(Collectors.toSet());
    }


    /**
     * @param start
     * @param end
     * @param mapByPosition
     * @return
     * @implNote end is exclusive
     */
    private Pair<Integer, Integer> extractBlockFromPosition(Integer start, Integer end,
                                                            ArrayList<PriorityQueue<TokenWrapper>> mapByPosition) {
        assert end - 1 >= start;
        TokenWrapper startToken = mapByPosition.get(start)
                                               .iterator()
                                               .next();
        var startOffset = startToken.startOffset;
        TokenWrapper endToken = mapByPosition.get(end - 1)
                                             .iterator()
                                             .next();
        var endOffset = endToken.endOffset;
        return Pair.of(startOffset, endOffset);
    }

    @Transactional
    public HighlightSingleSourceDTO getSingleSourceMatchById(String uuid) {
        HighlightSingleDocument singleDocument = repoHighlightSingleMatchDocument.findById(UUID.fromString(uuid))
                                                                                 .orElseThrow();
        return HighlightSingleSourceDTO.from(singleDocument, this::getHighlightWordMatchDTOS);
    }

    @Transactional
    public HighlightSessionDetailDTO getHighlightSessionById(String uuid) {
        Optional<HighlightSessionDocument> sessionDocumentById = repoHighlightSessionDocument.findById(
                UUID.fromString(uuid));
        HighlightSessionDocument resource = sessionDocumentById.orElseThrow(
                () -> new ResourceNotFoundException("Resource with uuid not found"));
        return HighlightSessionDetailDTO.from(resource);
    }

    @Nonnull
    private List<HighlightWordMatchDTO> getHighlightWordMatchDTOS(HighlightSingleTargetMatchDocument singleDocument) {
        FileDocument source = singleDocument.getSource()
                                            .getSource();
        FileDocument target = singleDocument.getTarget();
        MultiTermVectorsResponse multiTermVectors = repoElasticsearchQuery.getMultiTermVectors(source, target);
        return extractTermVectorsResponse(multiTermVectors);
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
                                                            .minimumShouldMatch("40%")
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
            UUID fromString;
            try {
                fromString = UUID.fromString(id);
            } catch (Exception ig) {
                continue;
            }
            Optional<FileDocument> fileDocument = repoFileDocument.findById(fromString);
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

    @Data
    private static class TokenWrapper {
        private static ObjectMapper mapper = new ObjectMapper();
        private Integer startOffset;
        private Integer endOffset;
        private Integer position;
        private String payload;
        private String token;

        private TokenWrapper() {
        }

        public Integer getLength() {
            return endOffset - startOffset;
        }

        public static TokenWrapper fromToken(String tokenValue, TermVectorsResponse.TermVector.Token token) {
            TokenWrapper tokenWrapper = mapper.convertValue(token, TokenWrapper.class);
            tokenWrapper.token = tokenValue;
            return tokenWrapper;
        }
    }

    public static class HighlightReturn {
        // pair is start and end offset
        @JsonProperty("source_block")
        Pair<Integer, Integer> sourceBlock;
        // each pair is start and end offset
        @JsonProperty("target_block")
        private List<Pair<Integer, Integer>> targetBlocks;
        @JsonIgnore
        private int longestCommonLength;
        @JsonProperty("has_match")
        final private boolean hasMatch;

        public HighlightReturn(int longestCommonLength) {
            this.longestCommonLength = longestCommonLength;
            hasMatch = longestCommonLength > 0;
            this.targetBlocks = new ArrayList<>();
        }

        public HighlightReturn sourceBlock(Pair<Integer, Integer> sourceBlock) {
            this.sourceBlock = sourceBlock;
            return this;
        }

        public HighlightReturn addAll(Collection<Pair<Integer, Integer>> blocks) {
            this.targetBlocks.addAll(blocks);
            return this;
        }

        public HighlightReturn targetBlock(Pair<Integer, Integer> block) {
            this.targetBlocks.add(block);
            return this;
        }
    }

    @Data
    private class HighlightProcessor implements Runnable {
        private final HighlightSessionDocument session;
        private final IndexInstruction instruction;

        public HighlightProcessor(HighlightSessionDocument session, IndexInstruction instruction) {
            this.session = session;
            this.instruction = instruction;
        }

        @Override
        @Transactional
        public void run() {
            try {
                markSessionAsProcessing();
                // Detect highlight session
                List<HighlightSingleDocument> hits = new ArrayList<>();
                for (FileDocument sourceDocument : instruction.getFiles()) {
                    // for each document, get highlight request
                    HighlightSingleDocument highlightSingleDocument = extractSingleDocument(sourceDocument);
                    hits.add(highlightSingleDocument);
                }
                session.setMatches(hits);
                session.setStatus(HighlightSessionStatus.DONE);
                repoHighlightSessionDocument.save(session);
                serviceIndex.indexAllDocuments(instruction);
            } catch (Exception e) {
                // Update status to failed for future retry
                session.setStatus(HighlightSessionStatus.FAILED);
                session.setException(
                        new HighlightSessionException("[Service highlight] Error while processing highlight",
                                e).toString());
                repoHighlightSessionDocument.save(session);
                log.error("[Service highlight] detect highlight session {} failed with error: {}", session.getName(),
                        e.getMessage());
            }
        }

        @Transactional
        public void markSessionAsProcessing() {
            try {
                session.setStatus(HighlightSessionStatus.PROCESSING);
                repoHighlightSessionDocument.save(session);
            } catch (Exception e) {
                log.error("[Service highlight] Can't update session to PROCESSING");
                session.setStatus(HighlightSessionStatus.FAILED);
                session.setException(
                        new HighlightSessionException("[Service highlight] Can't update session to PROCESSING",
                                e).toString());
            }
        }
    }
}