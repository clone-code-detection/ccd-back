package github.clone_code_detection.service.highlight;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import github.clone_code_detection.entity.authenication.UserImpl;
import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.entity.highlight.document.ReportSourceDocument;
import github.clone_code_detection.entity.highlight.document.ReportTargetDocument;
import github.clone_code_detection.entity.highlight.document.SimilarityReport;
import github.clone_code_detection.entity.highlight.dto.ReportSourceDocumentDTO;
import github.clone_code_detection.entity.highlight.dto.SimilarityReportDetailDTO;
import github.clone_code_detection.entity.highlight.dto.SimilarityTextMatchDTO;
import github.clone_code_detection.exceptions.highlight.ResourceNotFoundException;
import github.clone_code_detection.repo.RepoElasticsearchQuery;
import github.clone_code_detection.repo.RepoReportSourceDocument;
import github.clone_code_detection.repo.RepoReportTargetDocument;
import github.clone_code_detection.repo.RepoSimilarityReport;
import github.clone_code_detection.service.user.ServiceAuthentication;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.core.MultiTermVectorsResponse;
import org.elasticsearch.client.core.TermVectorsResponse;
import org.elasticsearch.common.util.set.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static github.clone_code_detection.repo.RepoElasticsearchQuery.SOURCE_CODE_FIELD;

@Service
@Validated
@Slf4j
@Transactional
public class ServiceHighlight {
    private final RepoElasticsearchQuery repoElasticsearchQuery;
    private final RepoSimilarityReport repoSimilarityReport;
    private final RepoReportSourceDocument repoReportSourceDocument;
    private final RepoReportTargetDocument repoReportTargetDocument;

    @Autowired
    public ServiceHighlight(RepoElasticsearchQuery repoElasticsearchQuery,
                            RepoSimilarityReport repoSimilarityReport,
                            RepoReportSourceDocument repoReportSourceDocument,
                            RepoReportTargetDocument repoReportTargetDocument) {
        this.repoElasticsearchQuery = repoElasticsearchQuery;
        this.repoSimilarityReport = repoSimilarityReport;
        this.repoReportSourceDocument = repoReportSourceDocument;
        this.repoReportTargetDocument = repoReportTargetDocument;
    }

    private static List<SimilarityTextMatchDTO> extractTermVectorsResponse(MultiTermVectorsResponse response) {
        List<SimilarityTextMatchDTO> res = new ArrayList<>();
        assert response.getTermVectorsResponses().size() == 2;

        TermVectorsResponse source = response.getTermVectorsResponses().get(0);
        TermVectorsResponse target = response.getTermVectorsResponses().get(1);
        // traverse every document in query
        Map<String, List<Integer[]>> sourceMap = extractMatches(source);
        Map<String, List<Integer[]>> targetMap = extractMatches(target);
        Set<String> commonKey = Sets.intersection(sourceMap.keySet(), targetMap.keySet());
        for (String common : commonKey) {
            SimilarityTextMatchDTO wordMatchDTO = SimilarityTextMatchDTO.builder()
                                                                        .text(common)
                                                                        .sourceMatches(sourceMap.get(common))
                                                                        .targetMatches(targetMap.get(common))
                                                                        .build();
            res.add(wordMatchDTO);
        }
        return res;
    }

    private static Map<String, List<Integer[]>> extractMatches(TermVectorsResponse termVectorsResponse) {
        Map<String, List<Integer[]>> res = new HashMap<>();
        TermVectorsResponse.TermVector termVector = getTermVectorByFieldName(termVectorsResponse.getTermVectorsList(),
                                                                             SOURCE_CODE_FIELD);
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

    private static TermVectorsResponse.TermVector getTermVectorByFieldName(List<TermVectorsResponse.TermVector> termVectors,
                                                                           String fieldName) {
        for (TermVectorsResponse.TermVector termVector : termVectors) {
            if (termVector.getFieldName().equals(fieldName))
                return termVector;
        }
        return null;
    }

    // Sorted lists
    private static <T extends Comparable<T>> boolean containsAny(List<T> a, List<T> b) {
        for (T t : b) {
            if (Collections.binarySearch(a, t) >= 0)
                return true;
        }
        return false;
    }

    private static Set<Integer> getTargetStartingPositions(List<PriorityQueue<TokenWrapper>> sourceMapByPosition,
                                                           Map<String, List<Integer>> targetByValue,
                                                           int i) {
        PriorityQueue<TokenWrapper> synonyms = sourceMapByPosition.get(i);
        return synonyms.stream().map(TokenWrapper::getToken).flatMap(tokenValue -> {
            if (!targetByValue.containsKey(tokenValue))
                return Stream.empty();
            return targetByValue.get(tokenValue).stream();
        }).collect(Collectors.toSet());
    }

    public Collection<HighlightReturn> handleAdvancedHighlightById(String id) {
        if (id.equals("undefined"))
            return new ArrayList<>();
        ReportTargetDocument singleDocument = repoReportTargetDocument.findById(UUID.fromString(id)).orElseThrow();
        return handleAdvancedHighlight(singleDocument);
    }

    /**
     * @return Map with token as string, and SORTED LIST of position as value
     */
    public Map<String, List<Integer>> mapTokensByValue(@NonNull TermVectorsResponse termVectorsResponse) {
        TermVectorsResponse.TermVector termVector = getTermVectorByFieldName(termVectorsResponse.getTermVectorsList(),
                                                                             SOURCE_CODE_FIELD);
        if (termVector == null)
            throw new RuntimeException();
        Map<String, List<Integer>> res = new HashMap<>();
        for (TermVectorsResponse.TermVector.Term term : termVector.getTerms()) {
            String termValue = term.getTerm();
            res.computeIfAbsent(termValue, value -> new ArrayList<>());
            var list = res.get(termValue);
            for (TermVectorsResponse.TermVector.Token token : term.getTokens())
                list.add(token.getPosition());
            list.sort(Integer::compareTo);
        }
        return res;
    }

    // PriorityQueue is ordered by term length
    public List<PriorityQueue<TokenWrapper>> mapTokensByPosition(@NonNull TermVectorsResponse termVectorsResponse) {
        TermVectorsResponse.TermVector termVector = getTermVectorByFieldName(termVectorsResponse.getTermVectorsList(),
                                                                             SOURCE_CODE_FIELD);
        if (termVector == null)
            throw new RuntimeException();
        int size = termVector.getTerms()
                             .stream()
                             .flatMap(term -> term.getTokens().stream())
                             .map(TermVectorsResponse.TermVector.Token::getPosition)
                             .max(Comparator.naturalOrder())
                             .orElseThrow();
        ArrayList<PriorityQueue<TokenWrapper>> res = new ArrayList<>();
        for (int i = 0; i <= size; i++)
            res.add(new PriorityQueue<>(Comparator.comparing(TokenWrapper::getLength).reversed()));

        for (TermVectorsResponse.TermVector.Term term : termVector.getTerms()) {
            var tokenValue = term.getTerm();
            for (TermVectorsResponse.TermVector.Token token : term.getTokens()) {
                TokenWrapper tokenWrapper = TokenWrapper.fromToken(tokenValue, token);
                Integer position = tokenWrapper.getPosition();
                res.get(position).add(tokenWrapper);
            }
        }
        return res;
    }

    /**
     * @return list of extract return
     *
     * @implNote: requires field mapping to have term vector position offset
     */
    public Collection<HighlightReturn> handleAdvancedHighlight(ReportTargetDocument targetMatchDocument) {
        var source = targetMatchDocument.getSource().getSource();
        var target = targetMatchDocument.getTarget();
        MultiTermVectorsResponse multiTermVectors = repoElasticsearchQuery.getMultiTermVectors(source, target);
        List<TermVectorsResponse> termVectorsResponses = multiTermVectors.getTermVectorsResponses();
        assert termVectorsResponses.size() == 2;
        var sourceTermVectorResponse = termVectorsResponses.get(0);
        var targetTermVectorResponse = termVectorsResponses.get(1);

        return getExtractReturnCollection(sourceTermVectorResponse, targetTermVectorResponse);
    }

    public Collection<HighlightReturn> getExtractReturnCollection(TermVectorsResponse source,
                                                                  TermVectorsResponse target) {
        // map source tokens by position
        List<PriorityQueue<TokenWrapper>> sourceMapByPosition = mapTokensByPosition(source);
        // map tokens by position
        List<PriorityQueue<TokenWrapper>> targetMapByPosition = mapTokensByPosition(target);
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

    private boolean condition(Collection<TokenWrapper> target, Collection<TokenWrapper> source) {
        return containsAny(target.stream().map(TokenWrapper::getToken).sorted().collect(Collectors.toList()),
                           source.stream().map(TokenWrapper::getToken).sorted().collect(Collectors.toList()));
    }

    private HighlightReturn extracted(List<PriorityQueue<TokenWrapper>> sourceMapByPosition,
                                      List<PriorityQueue<TokenWrapper>> targetMapByPosition,
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

                if (sourceStartingPosition >= sourceSize)
                    break;
                if (targetEndingPosition >= targetSize)
                    break;

                sourceSynonyms = sourceMapByPosition.get(sourceStartingPosition);
                targetSynonyms = targetMapByPosition.get(targetEndingPosition);
            }
            if (commonLength > maxCommonLength)
                maxCommonLength = commonLength;

            pairs.add(Pair.of(targetStartingPosition, targetEndingPosition));
        }
        HighlightReturn res = new HighlightReturn(maxCommonLength);
        for (Pair<Integer, Integer> tagetMatchPair : pairs) {
            int length = tagetMatchPair.getSecond() - tagetMatchPair.getFirst();
            if (maxCommonLength == length) {
                var matchBlock = extractBlockFromPosition(tagetMatchPair.getFirst(),
                                                          tagetMatchPair.getSecond(),
                                                          targetMapByPosition);
                res.targetBlock(matchBlock);
            }
        }
        var sourceMatchBlock = extractBlockFromPosition(i, maxCommonLength + i, sourceMapByPosition);
        res.sourceBlock(sourceMatchBlock);
        return res;
    }

    /**
     * @param start         start index inclusive
     * @param end           end index exclusive
     * @param mapByPosition map
     * @return pair
     */
    private Pair<Integer, Integer> extractBlockFromPosition(Integer start,
                                                            Integer end,
                                                            List<PriorityQueue<TokenWrapper>> mapByPosition) {
        assert end - 1 >= start;
        TokenWrapper startToken = mapByPosition.get(start).iterator().next();
        var startOffset = startToken.startOffset;
        TokenWrapper endToken = mapByPosition.get(end - 1).iterator().next();
        var endOffset = endToken.endOffset;
        return Pair.of(startOffset, endOffset);
    }

    @Transactional
    public ReportSourceDocumentDTO getReportSourceDocumentById(String uuid) {
        ReportSourceDocument singleDocument = repoReportSourceDocument.findById(UUID.fromString(uuid)).orElseThrow();
        return ReportSourceDocumentDTO.from(singleDocument, this::getReportTextMatch);
    }

    @Transactional
    public SimilarityReportDetailDTO getSimilarityReportById(String uuid) {
        Optional<SimilarityReport> sessionDocumentById = repoSimilarityReport.findById(UUID.fromString(uuid));
        SimilarityReport resource = sessionDocumentById.orElseThrow(() -> new ResourceNotFoundException(
                "Resource with uuid not found"));
        return SimilarityReportDetailDTO.from(resource);
    }

    @Nonnull
    private List<SimilarityTextMatchDTO> getReportTextMatch(ReportTargetDocument singleDocument) {
        FileDocument source = singleDocument.getSource().getSource();
        FileDocument target = singleDocument.getTarget();
        MultiTermVectorsResponse multiTermVectors = repoElasticsearchQuery.getMultiTermVectors(source, target);
        return extractTermVectorsResponse(multiTermVectors);
    }

    @Transactional
    public Collection<SimilarityReport.SimilarityReportDTO> getAllReports() {
        UserImpl principal = ServiceAuthentication.getUserFromContext();
        assert principal != null;
        return repoSimilarityReport.getAllByUserId(principal.getId());
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

        public Integer getLength() {
            return endOffset - startOffset;
        }
    }

    public static class HighlightReturn {
        @JsonProperty("has_match")
        private final boolean hasMatch;
        // pair is start and end offset
        @JsonProperty("source_block")
        Pair<Integer, Integer> sourceBlock;
        // each pair is start and end offset
        @JsonProperty("target_block")
        private List<Pair<Integer, Integer>> targetBlocks;
        @JsonIgnore
        private int longestCommonLength;

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

        public void targetBlock(Pair<Integer, Integer> block) {
            this.targetBlocks.add(block);
        }
    }

}