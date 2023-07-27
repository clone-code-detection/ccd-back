package github.clone_code_detection.service.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import github.clone_code_detection.entity.authenication.UserImpl;
import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.entity.highlight.document.ReportSourceDocument;
import github.clone_code_detection.entity.highlight.document.ReportTargetDocument;
import github.clone_code_detection.entity.highlight_intra.document.AuthorReport;
import github.clone_code_detection.entity.highlight_intra.document.IntraProjectReport;
import github.clone_code_detection.entity.query.QueryInstruction;
import github.clone_code_detection.repo.RepoElasticsearchIndex;
import github.clone_code_detection.repo.RepoElasticsearchQuery;
import github.clone_code_detection.repo.RepoFileDocument;
import github.clone_code_detection.repo.intra_project.RepoUserEsIndex;
import github.clone_code_detection.service.user.ServiceAuthentication;
import github.clone_code_detection.util.FileSystemUtil;
import github.clone_code_detection.util.ZipUtil;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.core.MultiTermVectorsRequest;
import org.elasticsearch.client.core.MultiTermVectorsResponse;
import org.elasticsearch.client.core.TermVectorsRequest;
import org.elasticsearch.client.core.TermVectorsResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static github.clone_code_detection.repo.RepoElasticsearchQuery.SOURCE_CODE_FIELD;
import static github.clone_code_detection.repo.RepoElasticsearchQuery.SOURCE_CODE_FIELD_NORMALIZED;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
class IntraProjectFileDocument {
    private static final ObjectMapper mapper = new ObjectMapper();

    @JsonProperty("source_code")
    String sourceCode;

    @JsonProperty("source_code_normalized")
    String sourceCodeNormalized;

    @JsonProperty("author")
    String author;

    @JsonIgnore
    String id;

    public String asJson() throws JsonProcessingException {
        return mapper.writeValueAsString(this);
    }

    public static IntraProjectFileDocument fromFileDocument(FileDocument fileDocument) {
        String contentAsString = fileDocument.getContentAsString();
        String esId = String.valueOf(fileDocument.getId());
        return IntraProjectFileDocument.builder()
                .sourceCode(contentAsString)
                .sourceCodeNormalized(contentAsString)
                .author(fileDocument.getAuthor())
                .id(esId)
                .build();
    }
}

@Service
@Slf4j
public class ServiceIntraProjectQuery {
    public static final ZoneId CURRENT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final RepoFileDocument repoFileDocument;
    private final RepoElasticsearchIndex repoElasticsearchIndex;
    private final RepoElasticsearchQuery repoElasticsearchQuery;
    private final ServiceQuery serviceQuery;
    private final RepoUserEsIndex repoUserEsIndex;


    private final ServiceIntraProjectQuery proxy;

    @Autowired
    public ServiceIntraProjectQuery(RepoFileDocument repoFileDocument,
                                    RepoElasticsearchIndex repoElasticsearchQuery,
                                    RepoElasticsearchQuery repoElasticsearchQuery1,
                                    @Lazy ServiceIntraProjectQuery proxy, // https://stackoverflow.com/questions/73000572/spring-async-method-called-from-within-the-class/73007337#73007337
                                    ServiceQuery serviceQuery, RepoUserEsIndex repoUserEsIndex) {
        this.repoUserEsIndex = repoUserEsIndex;
        this.repoFileDocument = repoFileDocument;
        this.repoElasticsearchIndex = repoElasticsearchQuery;
        this.repoElasticsearchQuery = repoElasticsearchQuery1;
        this.serviceQuery = serviceQuery;
        this.proxy = proxy;
    }

    @Data
    @Builder
    public static class IntraProjectQueryRequest {
        MultipartFile[] files; // Each file is a student submission, temp files only
        String projectName;
        String note;
    }

    @Transactional(propagation = Propagation.NEVER)
    public void queryIntraProject(IntraProjectQueryRequest request, QueryInstruction queryInstruction) throws IOException {
        validateZipFiles(request);
        IntraProjectReport report = createIntraProjectReport();
        List<FileDocument> fileDocuments = persistFile(request);
        proxy.handleQuery(report, fileDocuments, queryInstruction);
    }

    private static void validateZipFiles(IntraProjectQueryRequest request) {
        if (request.files == null) throw new RuntimeException();
        for (MultipartFile file : request.files) {
            FileSystemUtil.validateZip(file);
        }
    }

    private IntraProjectReport createIntraProjectReport() {
        IntraProjectReport progress = IntraProjectReport.builder().build();
        UserImpl user = ServiceAuthentication.getUserFromContext();
        progress.setUser(user);
        progress = repoUserEsIndex.save(progress);
        return progress;
    }

    @Async
    public void handleQuery(IntraProjectReport progress, List<FileDocument> fileDocuments, QueryInstruction queryInstruction) {
        try {
            // step 1. create index
            updateStatus(progress, "setup");
            String indexName = "java_" + progress.getUser().getId().toString() + "_" + System.currentTimeMillis();
            repoElasticsearchIndex.createIndex(indexName);
            progress.setEsIndex(indexName);

            // step 2. persist
            updateStatus(progress, "persist");
            fileDocuments = repoFileDocument.saveAll(fileDocuments);
            Set<String> authors = fileDocuments.stream().map(FileDocument::getAuthor).collect(Collectors.toSet());

            // step 3. index
            updateStatus(progress, "index");
            BulkRequest bulkIndexRequest = createBulkIndexRequest(indexName, fileDocuments);
            repoElasticsearchIndex.bulkIndex(bulkIndexRequest);

            // step 4. query
            updateStatus(progress, "query");
            List<ReportSourceDocument> hits = new ArrayList<>();
            MultiSearchRequest multiSearchRequest = new MultiSearchRequest();
            for (FileDocument sourceDocument : fileDocuments) {
                // for each document, get highlight request
                QueryInstruction instruction = queryInstruction.clone();
                instruction.setQueryDocument(sourceDocument);
                SearchRequest searchRequest = buildSearchRequest(indexName, instruction);
                multiSearchRequest.add(searchRequest);
            }
            MultiSearchResponse multiSearchResponse = repoElasticsearchQuery.multiQuery(multiSearchRequest);
            for (int index = 0; index < multiSearchResponse.getResponses().length; ++index) {
                FileDocument fileDocument = fileDocuments.get(index);
                MultiSearchResponse.Item searchResponse = multiSearchResponse.getResponses()[index];
                if (searchResponse.isFailure()) {
                    log.error("[Service highlight] Search response in multi highlight is fail. Error: {}",
                            searchResponse.getFailureMessage());
                } else if (searchResponse.getResponse() != null) {
                    ReportSourceDocument reportSourceDocument = serviceQuery.parseResponse(fileDocument, searchResponse.getResponse());
                    hits.add(reportSourceDocument);
                }
            }

            // step 5. calculate score
            updateStatus(progress, "score");
            Collection<String> ids = fileDocuments.stream().map(FileDocument::getId).map(UUID::toString).toList();
            MultiTermVectorsRequest multiTermVectorsRequest = buildMultiTermVectorRequest(indexName, ids);
            MultiTermVectorsResponse multiTermVectorsResponse = repoElasticsearchQuery.getMultiTermVectorsResponse(multiTermVectorsRequest);
            Map<String, TermVectorsResponse> termVectorsResponseMap = multiTermVectorsResponse
                    .getTermVectorsResponses()
                    .stream()
                    .collect(Collectors.toMap(TermVectorsResponse::getId, e -> e));
            for (ReportSourceDocument hit : hits) {
                TermVectorsResponse sourceTermVectorResponse = termVectorsResponseMap.get(hit.getSource().getId().toString());
                List<ReportTargetDocument> matches = hit.getMatches();
                List<TermVectorsResponse> targetTermVectorResponse = matches
                        .stream()
                        .map(ReportTargetDocument::getTarget)
                        .map(FileDocument::getId)
                        .map(UUID::toString)
                        .map(termVectorsResponseMap::get)
                        .toList();
                Double[] calculatePercentageMatch = serviceQuery.calculatePercentageMatch(sourceTermVectorResponse, targetTermVectorResponse);
                for (int i = 0; i < calculatePercentageMatch.length; i++) {
                    matches.get(i).setPercentageMatch(calculatePercentageMatch[i]);
                }
            }

            //step 6. calculate score for each author
            for (String author : authors) {
                Map<String, Integer> otherAuthorCount = new HashMap<>();
                List<ReportSourceDocument> sources = hits.stream()
                        .filter(reportSourceDocument -> reportSourceDocument.getSource().getAuthor().equals(author))
                        .toList();
                List<ReportTargetDocument> targets = sources
                        .stream().flatMap(reportSourceDocument -> reportSourceDocument.getMatches().stream())
                        .toList();
                for (ReportTargetDocument target : targets) {
                    String otherAuthor = target.getTarget().getAuthor();
                    if (!otherAuthorCount.containsKey(otherAuthor)) otherAuthorCount.put(otherAuthor, 0);
                    otherAuthorCount.put(otherAuthor, otherAuthorCount.get(otherAuthor) + 1);
                }
                AuthorReport.AuthorReportBuilder builder = AuthorReport.builder()
                        .author(author)
                        .totalFiles(sources.size())
                        .sources(sources);
                Optional<String> max = otherAuthorCount.keySet().stream().max(Comparator.comparing(otherAuthorCount::get));
                if (max.isPresent()) {
                    String otherAuthor = max.get();
                    builder.other_author(otherAuthor);
                    builder.totalMatches(otherAuthorCount.get(otherAuthor));
                }
                AuthorReport authorReport = builder.build();
                progress.addAuthorReport(authorReport);
            }
            progress = repoUserEsIndex.save(progress);

            // step 7. done
            updateStatus(progress, "done");
        } catch (Exception ex) {
            log.error("[Intra project query] Unknown error", ex);
            updateStatus(progress, "FAILED");
        }
    }

    @NonNull
    private static List<FileDocument> persistFile(IntraProjectQueryRequest request) {
        return Arrays.stream(request.files)
                .flatMap(file -> {
                    String author = file.getOriginalFilename();
                    byte[] bytes = new byte[0];
                    try {
                        bytes = file.getBytes();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return ZipUtil.unzipAndGetContents(bytes).stream().map(fileDocument -> {
                        fileDocument.setAuthor(author);
                        return fileDocument;
                    });
                })
                .filter(fileDocument -> fileDocument.getFileName().endsWith(".java")) // TODO get java only
                .toList();
    }

    private void updateStatus(IntraProjectReport progress, String status) {
        progress.setStatus(status);
        progress.setUpdatedAt(ZonedDateTime.now(CURRENT_ZONE));
        repoUserEsIndex.save(progress);
    }

    TermVectorsRequest buildTermVectorsRequest(String index, String id) {
        TermVectorsRequest template = new TermVectorsRequest(index, id);
        template.setOffsets(true);
        template.setPositions(true);
        template.setFieldStatistics(false);
        template.setFields(SOURCE_CODE_FIELD);
        return template;
    }

    MultiTermVectorsRequest buildMultiTermVectorRequest(String index, Collection<String> ids) {
        MultiTermVectorsRequest request = new MultiTermVectorsRequest();
        ids.stream().map(id -> buildTermVectorsRequest(index, id)).forEach(request::add);
        return request;
    }

    private SearchRequest buildSearchRequest(String indexName, QueryInstruction queryInstruction) {
        FileDocument fileDocument = queryInstruction.getQueryDocument();
        List<QueryBuilder> mustQuery = buildMustQuery(queryInstruction);
        List<QueryBuilder> filterQuery = buildFilterQuery(fileDocument);


        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        mustQuery.forEach(boolQueryBuilder::must);
        filterQuery.forEach(boolQueryBuilder::mustNot);

        // build source
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder);

        SearchRequest searchRequest = new SearchRequest();
        log.info("[Intra project query es] Search request: {}", searchRequest);
        searchRequest.source(searchSourceBuilder).indices(indexName);
        return searchRequest;
    }

    private BulkRequest createBulkIndexRequest(String index, Collection<FileDocument> fileDocuments) {
        BulkRequest request = new BulkRequest().setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        fileDocuments.stream().map(f -> {
            try {
                return createIndexRequest(index, f);
            } catch (JsonProcessingException e) {
                log.error("[Intra project query] Error creating json for {}", f.getFileName(), e);
                throw new RuntimeException(e);
            }
        }).forEach(request::add);
        return request;
    }

    private IndexRequest createIndexRequest(String index, FileDocument fileDocument) throws JsonProcessingException {
        IntraProjectFileDocument doc = IntraProjectFileDocument.fromFileDocument(fileDocument);
        IndexRequest source = new IndexRequest(index).source(doc.asJson(), XContentType.JSON);
        if (doc.getId() != null) source.id(doc.getId());
        return source;
    }

    public static List<QueryBuilder> buildMustQuery(QueryInstruction queryInstruction) {
        String field;
        log.debug("Querying with config: field {} and language {}",
                queryInstruction.getType(),
                queryInstruction.getLanguage());
        if (1 == queryInstruction.getType()) field = SOURCE_CODE_FIELD;
        else field = SOURCE_CODE_FIELD_NORMALIZED;
        return Collections.singletonList(QueryBuilders
                .matchQuery(field, queryInstruction.getContent())
                .minimumShouldMatch("10 < 40%")
        );
    }

    public static List<QueryBuilder> buildFilterQuery(FileDocument fileDocument) {
        return Collections.singletonList(QueryBuilders
                .matchQuery("author", fileDocument.getAuthor()));
    }
}
