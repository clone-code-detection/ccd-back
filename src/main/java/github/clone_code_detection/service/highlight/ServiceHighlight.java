package github.clone_code_detection.service.highlight;

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
import github.clone_code_detection.exceptions.highlight.ElasticsearchMultiHighlightException;
import github.clone_code_detection.exceptions.highlight.ElasticsearchQueryException;
import github.clone_code_detection.exceptions.highlight.HighlightSessionException;
import github.clone_code_detection.exceptions.highlight.ResourceNotFoundException;
import github.clone_code_detection.repo.*;
import github.clone_code_detection.service.index.ServiceIndex;
import github.clone_code_detection.util.FileSystemUtil;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.core.MultiTermVectorsResponse;
import org.elasticsearch.client.core.TermVectorsResponse;
import org.elasticsearch.common.util.set.Sets;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${elasticsearch.query.batch-size}")
    private int batchSize;
    @Value("${elasticsearch.query.minimum-should-match}")
    private String minimumShouldMatch;

    @Autowired
    public ServiceHighlight(ServiceIndex serviceIndex,
                            RepoElasticsearchQuery repoElasticsearchQuery,
                            RepoHighlightSessionDocument repoHighlightSessionDocument,
                            RepoHighlightSingleMatchDocument repoHighlightSingleMatchDocument,
                            RepoHighlightSingleTargetMatchDocument repoHighlightSingleTargetMatchDocument,
                            RepoFileDocument repoFileDocument) {
        this.serviceIndex = serviceIndex;
        this.repoElasticsearchQuery = repoElasticsearchQuery;
        this.repoHighlightSessionDocument = repoHighlightSessionDocument;
        this.repoHighlightSingleMatchDocument = repoHighlightSingleMatchDocument;
        this.repoHighlightSingleTargetMatchDocument = repoHighlightSingleTargetMatchDocument;
        this.repoFileDocument = repoFileDocument;
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
            if (termVector.getFieldName().equals(fieldName)) return termVector;
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

    public Collection<ExtractReturn> handleAdvancedHighlightById(String id) {
        if (id.equals("undefined"))
            return new ArrayList<>();
        HighlightSingleTargetMatchDocument singleDocument = repoHighlightSingleTargetMatchDocument
                .findById(UUID.fromString(id))
                .orElseThrow();
        return handleAdvancedHighlight(singleDocument);
    }

    /**
     * @return Map with token as string, and SORTED LIST of position as value
     */
    public Map<String, List<Integer>> mapTokensByValue(@NonNull TermVectorsResponse termVectorsResponse) {
        TermVectorsResponse.TermVector termVector = getTermVectorByFieldName(termVectorsResponse.getTermVectorsList(), SOURCE_CODE_FIELD);
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

    public ArrayList<LinkedHashSet<TokenWrapper>> mapTokensByPosition(@NonNull TermVectorsResponse
                                                                              termVectorsResponse) {
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
        ArrayList<LinkedHashSet<TokenWrapper>> res = new ArrayList<>();
        for (int i = 0; i <= size; i++) res.add(new LinkedHashSet<>());

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
    public Collection<ExtractReturn> handleAdvancedHighlight(HighlightSingleTargetMatchDocument targetMatchDocument) {
        var source = targetMatchDocument.getSource().getSource();
        var target = targetMatchDocument.getTarget();
        MultiTermVectorsResponse multiTermVectors = repoElasticsearchQuery.getMultiTermVectors(source, target);
        List<TermVectorsResponse> termVectorsResponses = multiTermVectors.getTermVectorsResponses();
        assert termVectorsResponses.size() == 2;
        var sourceTermVectorResponse = termVectorsResponses.get(0);
        var targetTermVectorResponse = termVectorsResponses.get(1);

        // map source tokens by position
        ArrayList<LinkedHashSet<TokenWrapper>> sourceMapByPosition = mapTokensByPosition(sourceTermVectorResponse);
        // map tokens by position
        ArrayList<LinkedHashSet<TokenWrapper>> targetMapByPosition = mapTokensByPosition(targetTermVectorResponse);
        // map tokens by value
        Map<String, List<Integer>> targetByValue = mapTokensByValue(targetTermVectorResponse);

        for (LinkedHashSet<TokenWrapper> tokenWrappers : targetMapByPosition) {
            TokenWrapper tokenWrapper = tokenWrappers.iterator().next();
        }

        // logic
        Collection<ExtractReturn> res = new ArrayList<>();
        int i = 0;
        while (i < sourceMapByPosition.size()) {
            ExtractReturn extracted = extracted(sourceMapByPosition, targetMapByPosition, targetByValue, i);
            i += extracted.longestCommonLength;
            res.add(extracted);
        }
        return res;
    }

    private ExtractReturn extracted(ArrayList<LinkedHashSet<TokenWrapper>> sourceMapByPosition,
                                    ArrayList<LinkedHashSet<TokenWrapper>> targetMapByPosition,
                                    Map<String, List<Integer>> targetByValue,
                                    Integer i) {
        Set<TokenWrapper> synonyms = sourceMapByPosition.get(i);
        List<Pair<Integer, Integer>> pairs = new ArrayList<>();
        int longestCommon = Integer.MIN_VALUE;

        Set<Integer> targetStartingPositions = synonyms.stream()
                .map(TokenWrapper::getToken)
                .flatMap(tokenValue -> targetByValue.get(tokenValue)
                        .stream())
                .collect(Collectors.toSet());
        for (Integer targetStartingPosition : targetStartingPositions) {
            int commonLength = 0;
            Integer targetEndingPosition = targetStartingPosition;
            Integer sourceEndingPosition = i;
            while (condition(sourceEndingPosition, targetEndingPosition, sourceMapByPosition, targetMapByPosition)) {
                targetEndingPosition++;
                sourceEndingPosition++;
                commonLength++;
            }
            if (commonLength <= longestCommon) continue;
            else longestCommon = commonLength;
            if (commonLength != 0) targetEndingPosition--;
            pairs.add(Pair.of(targetStartingPosition, targetEndingPosition));
        }

        final int finalLongestCommon = longestCommon;
        // Only extract the longest blocks with same length
        Set<Pair<Integer, Integer>> filteredLongest = pairs.stream()
                .filter(pair -> pair.getSecond() - pair.getFirst() + 1
                        == finalLongestCommon) // pair is only position index
                .map(positionPair -> extractBlockFromPosition(positionPair,
                        targetMapByPosition))
                .collect(Collectors.toSet());
        return new ExtractReturn(finalLongestCommon).addAll(filteredLongest)
                .sourceBlock(extractBlockFromPosition(i, i + finalLongestCommon - 1,
                        sourceMapByPosition));
    }

    private Pair<Integer, Integer> extractBlockFromPosition(Integer start, Integer end,
                                                            ArrayList<LinkedHashSet<TokenWrapper>> mapByPosition) {
        TokenWrapper startToken = mapByPosition.get(start)
                .iterator()
                .next();
        var startOffset = startToken.startOffset;
        TokenWrapper endToken = mapByPosition.get(end)
                .iterator()
                .next();
        var endOffset = endToken.endOffset;
        return Pair.of(startOffset, endOffset);
    }

    private Pair<Integer, Integer> extractBlockFromPosition(Pair<Integer, Integer> positionPair,
                                                            ArrayList<LinkedHashSet<TokenWrapper>> mapByPosition) {
        var start = positionPair.getFirst();
        var end = positionPair.getSecond();
        return this.extractBlockFromPosition(start, end, mapByPosition);
    }

    private boolean condition(Integer sourcePosition, Integer targetPosition,
                              ArrayList<LinkedHashSet<TokenWrapper>> sourceMapByPosition,
                              ArrayList<LinkedHashSet<TokenWrapper>> targetMapByPosition) {
        if (sourcePosition >= sourceMapByPosition.size() || targetPosition >= targetMapByPosition.size())
            return false;
        Set<String> sourceTokens = sourceMapByPosition.get(sourcePosition)
                .stream()
                .map(TokenWrapper::getToken)
                .collect(Collectors.toSet());
        Set<String> targetTokens = targetMapByPosition.get(targetPosition)
                .stream()
                .map(TokenWrapper::getToken)
                .collect(Collectors.toSet());

        for (String sourceToken : sourceTokens) {
            if (targetTokens.contains(sourceToken)) return true;
        }
        return false;
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
                .minimumShouldMatch(minimumShouldMatch)
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

    public Collection<HighlightSingleDocument> multihighlight(Collection<FileDocument> files) {
        Collection<HighlightSingleDocument> highlightSingleDocuments = new ArrayList<>();
        int startIndex = 0;
        while (startIndex < files.size()) {
            int endIndex = Math.min(batchSize + startIndex, files.size());
            Collection<FileDocument> subFiles = files.stream().toList().subList(startIndex, endIndex);
            // Create multisearch query
            Collection<QueryInstruction> instructions = new ArrayList<>();
            subFiles.forEach(file -> instructions
                    .add(QueryInstruction
                            .builder()
                            .queryDocument(file)
                            .includeHighlight(true)
                            .minimumShouldMatch(minimumShouldMatch)
                            .build()));
            try {
                MultiSearchResponse multiSearchResponse = repoElasticsearchQuery.multiquery(instructions);
                for (int index = 0; index < multiSearchResponse.getResponses().length; ++index) {
                    MultiSearchResponse.Item searchResponse = multiSearchResponse.getResponses()[index];
                    if (searchResponse.isFailure()) {
                        log.error("[Service highlight] Search response in multi highlight is fail. Error: {}", searchResponse.getFailureMessage());
                    } else if (searchResponse.getResponse() != null) {
                        highlightSingleDocuments.add(parseResponse(subFiles.stream().toList().get(index), searchResponse.getResponse()));
                    }
                }
            } catch (IOException e) {
                log.error("[Service highlight] Multi highlight failed. Error: {}", e.getMessage());
                throw new ElasticsearchMultiHighlightException("Multi highlight failed", e);
            } finally {
                startIndex = endIndex;
            }
        }
        return highlightSingleDocuments;
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

        public static TokenWrapper fromToken(String tokenValue, TermVectorsResponse.TermVector.Token token) {
            TokenWrapper tokenWrapper = mapper.convertValue(token, TokenWrapper.class);
            tokenWrapper.token = tokenValue;
            return tokenWrapper;
        }
    }

    public static class ExtractReturn {
        // pair is start and end offset
        @JsonProperty("source_block")
        Pair<Integer, Integer> sourceBlock;
        // each pair is start and end offset
        @JsonProperty("target_block")
        private List<Pair<Integer, Integer>> targetBlocks;
        @JsonProperty("longest_common")
        private int longestCommonLength;

        public ExtractReturn(int longestCommonLength) {
            this.longestCommonLength = longestCommonLength;
            this.targetBlocks = new ArrayList<>();
        }

        public ExtractReturn sourceBlock(Pair<Integer, Integer> sourceBlock) {
            this.sourceBlock = sourceBlock;
            return this;
        }

        public ExtractReturn addAll(Collection<Pair<Integer, Integer>> blocks) {
            this.targetBlocks.addAll(blocks);
            return this;
        }

        public ExtractReturn add(Pair<Integer, Integer> block) {
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
                Collection<FileDocument> files = instruction.getFiles();
                List<HighlightSingleDocument> hits = new ArrayList<>(multihighlight(files));
                session.setMatches(hits);
                session.setStatus(HighlightSessionStatus.DONE);
                repoHighlightSessionDocument.save(session);
                serviceIndex.bulkIndexAllDocuments(instruction);
            } catch (Exception e) {
                // Update status to failed for future retry
                session.setStatus(HighlightSessionStatus.FAILED);
                session.setException(new HighlightSessionException("[Service highlight] Error while processing highlight", e).toString());
                repoHighlightSessionDocument.save(session);
                log.error("[Service highlight] detect highlight session {} failed with error: {}", session.getName(), e.getMessage());
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
                session.setException(new HighlightSessionException("[Service highlight] Can't update session to PROCESSING", e).toString());
            }
        }
    }
}