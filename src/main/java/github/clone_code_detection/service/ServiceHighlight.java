package github.clone_code_detection.service;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import github.clone_code_detection.entity.*;
import github.clone_code_detection.repo.RepoElasticsearchQuery;
import github.clone_code_detection.repositories.HighlightReportRepository;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ServiceHighlight extends ServiceQuery {
    final static ObjectMapper mapper = new ObjectMapper();
    final RepoElasticsearchQuery repoElasticsearchQuery;
    final HighlightReportRepository repository;
    @Value("${report.storage}")
    String localStorage;

    @Autowired
    public ServiceHighlight(RepoElasticsearchQuery repoElasticsearchQuery, HighlightReportRepository repository) {
        super(repoElasticsearchQuery);
        this.repoElasticsearchQuery = repoElasticsearchQuery;
        this.repository = repository;
    }

    @SneakyThrows
    public Collection<ElasticsearchDocument> highlight(QueryDocument queryDocument) {
        // Call elasticsearch to highlight
        // Build highlight request
        SearchRequest searchRequest = buildHighlightSearchRequest(queryDocument);
        // Log
        log.info("{}", searchRequest);

        // Query with highlight option
        return repoElasticsearchQuery.query(searchRequest)
                .hits()
                .hits()
                .stream()
                .map(hit -> ElasticsearchDocument
                        .builder()
                        .meta(hit.source() != null ? hit.source().getMeta() : null)
                        .sourceCode(hit.highlight().get("source_code").get(0))
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public HighlightReport generateHighlightReport(Collection<ElasticsearchDocument> documents, HighlightDocument request) {
        // Get highlight response
        HighlightResponse highlightResponse = buildHighlightResponse(documents, request);
        // Build report
        HighlightReport report = buildReport(request);
        if (report == null)
            return null;
        // Create report file and append it to report folder
        if (!createAndStoreReportFileIntoFolder(report, highlightResponse, request.getMetadata().getFilename()))
            return null;
        return repository.save(report);
    }

    private boolean createAndStoreReportFileIntoFolder(HighlightReport report, HighlightResponse highlightResponse, String filename) {
        // Create report file as json for specific file
        filename = filename + ".json";
        boolean result = writeObjectToJsonFile(Paths.get(report.getUri(), filename).toFile(), highlightResponse);
        if (!result)
            return false;
        if (!report.getExtraData().contains(filename)) {
            Set<String> temp = new java.util.HashSet<>(Set.copyOf(report.getExtraData()));
            if (temp.add(filename)) {
                report.setExtraData(temp);
                return true;
            }
            log.error("Can not add filename to report. Filename: {}", filename);
            return false;
        }
        return true;
    }

    private boolean writeObjectToJsonFile(File file, HighlightResponse body) {
        try {
            mapper.writeValue(file, body);
            return true;
        } catch (IOException e) {
            log.error("Can not write {} to storage\nError: {}", file.getName(), e.getMessage());
            return false;
        }
    }

    private HighlightReport buildReport(HighlightDocument request) {
        // Check if report folder has been created
        HighlightReport report = repository.findHighlightReportByOrganizationAndYearAndSemesterAndCourseAndAssignerAndProjectAndAuthor(
                request.getMetadata().getOrganization(),
                request.getMetadata().getYear(),
                request.getMetadata().getSemester(),
                request.getMetadata().getCourse(),
                request.getMetadata().getAssigner(),
                request.getMetadata().getProject(),
                request.getMetadata().getAuthor());
        if (report != null)
            return report;
        // Create folder for this project
        String folderPath = createFolderIfNotExists(request);
        if (folderPath == null) return null;

        // Create report
        return HighlightReport.builder()
                .organization(request.getMetadata().getOrganization())
                .year(request.getMetadata().getYear())
                .semester(request.getMetadata().getSemester())
                .course(request.getMetadata().getCourse())
                .assigner(request.getMetadata().getAssigner())
                .project(request.getMetadata().getProject())
                .author(request.getMetadata().getAuthor())
                .uri(folderPath)
                .extraData(new HashSet<>())
                .build();
    }

    private String createFolderIfNotExists(HighlightDocument request) {
        File organization = new File(Paths.get(localStorage, request.getMetadata().getOrganization()).toUri());
        if (!organization.exists() && !organization.mkdirs()) return null;

        File year = new File(Paths.get(organization.getAbsolutePath(), String.valueOf(request.getMetadata().getYear())).toUri());
        if (!year.exists() && !year.mkdirs()) return null;

        File semester = new File(Paths.get(year.getAbsolutePath(), String.valueOf(request.getMetadata().getSemester())).toUri());
        if (!semester.exists() && !semester.mkdirs()) return null;

        File course = new File(Paths.get(semester.getAbsolutePath(), request.getMetadata().getCourse()).toUri());
        if (!course.exists() && !course.mkdirs()) return null;

        File assigner = new File(Paths.get(course.getAbsolutePath(), request.getMetadata().getAssigner()).toUri());
        if (!assigner.exists() && !assigner.mkdirs()) return null;

        File project = new File(Paths.get(assigner.getAbsolutePath(), request.getMetadata().getProject()).toUri());
        if (!project.exists() && !project.mkdirs()) return null;

        File author = new File(Paths.get(project.getAbsolutePath(), request.getMetadata().getAuthor()).toUri());
        if (!author.exists() && !author.mkdirs()) return null;

        return author.getAbsolutePath();
    }

    private HighlightResponse buildHighlightResponse(Collection<ElasticsearchDocument> documents, HighlightDocument request) {
        return HighlightResponse.builder()
                .totalCount(documents.size())
                .origin(ElasticsearchDocument.builder().sourceCode(request.getContent()).build())
                .documents(documents)
                .build();
    }

    private SearchRequest buildHighlightSearchRequest(QueryDocument queryDocument) {
        // Get list of index table
        List<String> indexes = queryDocument.getLanguages().stream().toList();
        // build highlight request
        List<Query> mustQuery = buildMustQuery(queryDocument);
        List<Query> filterQuery = buildFilterQuery(queryDocument);
        Highlight highlight = buildHighlightFields();
        var query = BoolQuery.of(bp -> bp.filter(filterQuery).must(mustQuery))._toQuery();
        return SearchRequest.of(sr -> sr
                .index(indexes)
                .query(query)
                .highlight(highlight)
                .source(SourceConfig.of(source -> source.filter(SourceFilter.of(filter -> filter.excludes("source_code")))))
        );
    }

    private Highlight buildHighlightFields() {
        return Highlight.of(highlight -> highlight
                .fields("source_code",
                        new HighlightField.Builder()
                                .numberOfFragments(0)
                                .preTags("<mark>")
                                .postTags("</mark>")
                                .build()
                )
                .order(HighlighterOrder.Score)
        );
    }
}
