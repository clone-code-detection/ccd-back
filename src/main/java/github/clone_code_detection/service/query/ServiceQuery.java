package github.clone_code_detection.service.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import github.clone_code_detection.entity.authenication.UserImpl;
import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.entity.highlight.document.ReportSourceDocument;
import github.clone_code_detection.entity.highlight.document.ReportTargetDocument;
import github.clone_code_detection.entity.highlight.document.SimilarityReport;
import github.clone_code_detection.entity.highlight.document.SimilarityReportMeta;
import github.clone_code_detection.entity.highlight.dto.SimilarityReportDetailDTO;
import github.clone_code_detection.entity.highlight.request.SimilarityDetectRequest;
import github.clone_code_detection.entity.index.IndexInstruction;
import github.clone_code_detection.entity.query.QueryInstruction;
import github.clone_code_detection.exceptions.elasticsearch.OperationException;
import github.clone_code_detection.repo.RepoElasticsearchQuery;
import github.clone_code_detection.repo.RepoFileDocument;
import github.clone_code_detection.repo.RepoReportSourceDocument;
import github.clone_code_detection.repo.RepoSimilarityReport;
import github.clone_code_detection.service.highlight.ServiceHighlight;
import github.clone_code_detection.service.index.ServiceIndex;
import github.clone_code_detection.service.user.ServiceAuthentication;
import github.clone_code_detection.util.FileSystemUtil;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.core.MultiTermVectorsResponse;
import org.elasticsearch.client.core.TermVectorsResponse;
import org.elasticsearch.common.util.set.Sets;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ServiceQuery {
    private static final DecimalFormat decimalFormat = new DecimalFormat("#.##");
    private final RepoElasticsearchQuery repoElasticsearchQuery;
    private final RepoReportSourceDocument repoReportSourceDocument;
    private final RepoSimilarityReport repoSimilarityReport;
    private final RepoFileDocument repoFileDocument;
    private final ServiceIndex serviceIndex;
    private final ThreadPoolExecutor threadPoolExecutor;
    private final DetectSimilarityJobFactory detectSimilarityJobFactory;
    private final ServiceHighlight serviceHighlight;

    @Autowired
    public ServiceQuery(RepoElasticsearchQuery repoElasticsearchQuery,
                        RepoReportSourceDocument repoHighlightSessionDocument,
                        RepoSimilarityReport repoSimilarityReport,
                        RepoFileDocument repoFileDocument,
                        ServiceIndex serviceIndex,
                        ThreadPoolExecutor threadPoolExecutor,
                        @Lazy DetectSimilarityJobFactory detectSimilarityJobFactory,
                        ServiceHighlight serviceHighlight) {
        this.repoElasticsearchQuery = repoElasticsearchQuery;
        this.repoReportSourceDocument = repoHighlightSessionDocument;
        this.repoSimilarityReport = repoSimilarityReport;
        this.repoFileDocument = repoFileDocument;
        this.serviceIndex = serviceIndex;
        this.threadPoolExecutor = threadPoolExecutor;
        this.detectSimilarityJobFactory = detectSimilarityJobFactory;
        this.serviceHighlight = serviceHighlight;
    }

    private static TargetMatchOverview extract(ReportTargetDocument document) {
        String userName = document.getTarget().getAuthor();
        String targetId = document.getId().toString();
        double score = Double.parseDouble(decimalFormat.format(document.getScore()));
        String meta = String.join(", ",
                document.getTarget()
                        .getMeta()
                        .entrySet()
                        .stream()
                        .map(map -> String.format("%s: %s", map.getKey(), map.getValue()))
                        .toList());
        String filename = document.getTarget()
                .getFileName()
                .substring(document.getTarget().getFileName().lastIndexOf("/") + 1);
        return TargetMatchOverview.builder()
                .author(userName)
                .id(targetId)
                .score(score)
                .origin(document.getTarget().getOrigin())
                .originLink(document.getTarget().getOriginLink())
                .fileName(filename)
                .meta(meta)
                .percentageMatch(document.getPercentageMatch())
                .build();
    }

    public Collection<TargetMatchOverview> handle(String id) {
        UUID fromString;
        try {
            fromString = UUID.fromString(id);
        } catch (Exception ig) {
            throw new RuntimeException("Invalid id", ig);
        }
        ReportSourceDocument singleDocument = repoReportSourceDocument.findById(fromString)
                .orElseThrow();
        return singleDocument.getMatches()
                .stream()
                .map(ServiceQuery::extract)
                .toList();
    }

    /**
     * For each file, query with highlight enabled
     */
    public ReportSourceDocument extractSingleDocument(FileDocument source, QueryInstruction queryInstruction) {
        SearchResponse searchResponse;
        try {
            searchResponse = repoElasticsearchQuery.query(queryInstruction);
        } catch (IOException e) {
            log.error("Error querying elasticsearch", e);
            throw new OperationException("[Service highlight] Failed to query es");
        }
        // parse response
        return parseResponse(source, searchResponse);
    }

    /**
     * Extract match fields from es search response
     */
    public ReportSourceDocument parseResponse(FileDocument source, SearchResponse search) {
        ReportSourceDocument.ReportSourceDocumentBuilder builder = ReportSourceDocument.builder();
        // get hits
        List<ReportTargetDocument> matches = new ArrayList<>();
        ReportSourceDocument reportSourceDocument = builder.source(source)
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
            if (fileDocument.isEmpty())
                continue;
            ReportTargetDocument singleMatch = ReportTargetDocument.builder()
                    .score(hit.getScore())
                    .source(reportSourceDocument)
                    .target(fileDocument.get())
                    .build();
            matches.add(singleMatch);
        }
        return reportSourceDocument;
    }

    private Double[] calculatePercentageMatch(FileDocument source, List<FileDocument> queryMatches) {
        FileDocument[] array = new FileDocument[queryMatches.size() + 1];
        array[0] = source;
        for (int i = 0; i < queryMatches.size(); i++) {
            array[i + 1] = queryMatches.get(i);
        }

        MultiTermVectorsResponse multiTermVectors = repoElasticsearchQuery.getMultiTermVectors(null, array);
        List<TermVectorsResponse> termVectorsResponses = multiTermVectors.getTermVectorsResponses();
        return calculatePercentageMatch(termVectorsResponses.get(0), termVectorsResponses.subList(1, termVectorsResponses.size()));
    }

    public Double[] calculatePercentageMatch(TermVectorsResponse sourceTermVectorResponse, List<TermVectorsResponse> target) {
        int sourceCount = sourceTermVectorResponse.getTermVectorsList()
                .stream()
                .filter(Objects::nonNull)
                .flatMap(termVector -> termVector.getTerms()
                        .stream())
                .filter(Objects::nonNull)
                .flatMap(term -> term.getTokens()
                        .stream())
                .map(TermVectorsResponse.TermVector.Token::getPosition)
                .collect(Collectors.toSet())
                .size();
        Map<String, List<Integer>> sourceMap = serviceHighlight.mapTokensByValue(sourceTermVectorResponse);
        Double[] matchPercentage = new Double[target.size()];
        for (int i = 0; i < target.size(); i++) {
            var queryTermVectorResponse = target.get(i);
            Map<String, List<Integer>> queryMap = serviceHighlight.mapTokensByValue(queryTermVectorResponse);
            int matchCount = Sets.intersection(sourceMap.keySet(), queryMap.keySet())
                    .stream()
                    .flatMap(s -> sourceMap.get(s)
                            .stream())
                    .collect(Collectors.toSet())
                    .size();
            matchPercentage[i] = (matchCount * 1.0 / sourceCount);
        }
        return matchPercentage;
    }

    /**
     * @param sourceDocuments: Requires persisted objects
     */
    public void calculatePercentageMatches(Collection<ReportSourceDocument> sourceDocuments) {
        for (ReportSourceDocument source : sourceDocuments) {
            FileDocument sourceSource = source.getSource();
            List<FileDocument> queryMatches = source.getMatches()
                    .stream()
                    .map(ReportTargetDocument::getTarget)
                    .toList();
            assert sourceSource != null;
            Double[] calculatePercentageMatch = calculatePercentageMatch(sourceSource, queryMatches);
            int i = 0;
            for (ReportTargetDocument targetDocument : source.getMatches()) {
                targetDocument.setPercentageMatch(calculatePercentageMatch[i++]);
            }
        }
    }

    // query
    @Transactional
    public SimilarityReport createSimilarityReport(SimilarityDetectRequest request, QueryInstruction queryInstruction) {
        SimilarityReport similarityReport = createEmptyReport(request, queryInstruction);
        // Assign session id of empty highlight session into each source document
        return detectAndFulfillReport(request, queryInstruction, similarityReport);
    }

    @NotNull
    private SimilarityReport createEmptyReport(SimilarityDetectRequest request, QueryInstruction queryInstruction) {
        // Create new empty highlight session
        SimilarityReport.SimilarityReportBuilder sessionBuilder = SimilarityReport.builder();
        SimilarityReport similarityReport = sessionBuilder.build();
        similarityReport.setUser(ServiceAuthentication.getUserFromContext());
        similarityReport.setName(request.getReportName());
        similarityReport.setMeta(SimilarityReportMeta.builder()
                .author(request.getAuthor())
                .origin(request.getOrigin())
                .link(request.getLink())
                .minimumShouldMatch(queryInstruction.getMinimumShouldMatch())
                .type(queryInstruction.getType())
                .build());
        return repoSimilarityReport.save(similarityReport);
    }

    @Transactional
    public SimilarityReport createSimilarityReport(SimilarityDetectRequest request,
                                                   QueryInstruction queryInstruction,
                                                   UserImpl user) {
        SimilarityReport similarityReport = createEmptyReport(request, user, queryInstruction);
        // Assign session id of empty highlight session into each source document
        return detectAndFulfillReport(request, queryInstruction, similarityReport);
    }

    @NotNull
    private SimilarityReport detectAndFulfillReport(SimilarityDetectRequest request,
                                                    QueryInstruction queryInstruction,
                                                    SimilarityReport similarityReport) {
        Collection<FileDocument> sourceDocuments = request.getSources();
        // Save file linking id of session
        sourceDocuments = repoFileDocument.saveAll(sourceDocuments);
        IndexInstruction indexInstruction = IndexInstruction.builder()
                .files(sourceDocuments)
                .build();

        DetectSimilarityJob.DetectSimilarityJobBuilder builder = DetectSimilarityJob.builder()
                .instruction(indexInstruction)
                .report(similarityReport)
                .queryInstruction(queryInstruction);

        DetectSimilarityJob detectSimilarityJob = detectSimilarityJobFactory.newInstance(builder);
        threadPoolExecutor.submit(detectSimilarityJob);
        return similarityReport;
    }

    @NotNull
    @Transactional
    public SimilarityReport createEmptyReport(SimilarityDetectRequest request, UserImpl user,
                                              QueryInstruction queryInstruction) {
        // init meta
        SimilarityReportMeta meta = SimilarityReportMeta.builder()
                .author(request.getAuthor())
                .origin(request.getOrigin())
                .minimumShouldMatch(queryInstruction.getMinimumShouldMatch())
                .type(queryInstruction.getType())
                .link(request.getLink())
                .build();
        // Create new empty highlight session
        SimilarityReport.SimilarityReportBuilder sessionBuilder = SimilarityReport.builder();
        SimilarityReport similarityReport = sessionBuilder.build();
        similarityReport.setUser(user);
        similarityReport.setName(request.getReportName());
        similarityReport.setMeta(meta);
        return repoSimilarityReport.save(similarityReport);
    }

    @Transactional
    public SimilarityReportDetailDTO detectSync(@NotNull MultipartFile source, QueryInstruction queryInstruction,
                                                String reportName, JsonNode meta) {
        // Validate and extract file from source
        FileSystemUtil.validate(source);
        Collection<FileDocument> sourceDocuments = FileSystemUtil.extractDocuments(source, meta);
        // Highlight source documents
        SimilarityReport.SimilarityReportBuilder sessionBuilder = SimilarityReport.builder();
        List<ReportSourceDocument> hits = new ArrayList<>();
        for (FileDocument sourceDocument : sourceDocuments) {
            // for each document, get highlight request
            QueryInstruction instruction = queryInstruction.clone();
            instruction.setQueryDocument(sourceDocument);
            ReportSourceDocument reportSourceDocument = extractSingleDocument(sourceDocument, instruction);
            hits.add(reportSourceDocument);
        }
        // Build highlight session
        sessionBuilder.sources(hits);
        SimilarityReport similarityReport = sessionBuilder.build();
        similarityReport.setUser(ServiceAuthentication.getUserFromContext());
        similarityReport.setName(reportName);
        similarityReport.setMeta(SimilarityReportMeta.builder()
                .type(queryInstruction.getType())
                .minimumShouldMatch(
                        queryInstruction.getMinimumShouldMatch())
                .link(meta.get("origin_link").asText())
                .origin(meta.get("origin").asText())
                .author(meta.get("author").asText())
                .build());
        similarityReport = repoSimilarityReport.save(similarityReport);

        // Save source files before indexing
        sourceDocuments = repoFileDocument.saveAll(sourceDocuments);
        // Index the file into es
        IndexInstruction indexInstruction = IndexInstruction.builder()
                .files(sourceDocuments)
                .build();
        serviceIndex.indexAllDocuments(indexInstruction);
        Collection<ReportSourceDocument> sources = similarityReport.getSources();
        calculatePercentageMatches(sources);
        return SimilarityReportDetailDTO.from(similarityReport);
    }

    @Builder
    public static class TargetMatchOverview {
        @JsonProperty("author")
        private String author;

        @JsonProperty("score")
        private double score;

        @JsonProperty("origin")
        private String origin;

        @JsonProperty("origin_link")
        private String originLink;

        @JsonProperty("file_name")
        private String fileName;

        @JsonProperty("meta")
        private String meta;

        @JsonProperty("target_match_id")
        private String id;

        @JsonProperty("percentage_match")
        private double percentageMatch;
    }
}
