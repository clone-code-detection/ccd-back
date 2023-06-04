package github.clone_code_detection.service.query;

import github.clone_code_detection.repo.RepoElasticsearchQuery;
import github.clone_code_detection.repo.RepoSimilarityReport;
import github.clone_code_detection.service.index.ServiceIndex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DetectSimilarityJobFactory {

    private final ServiceIndex serviceIndex;
    private final RepoSimilarityReport repoSimilarityReport;
    private final RepoElasticsearchQuery repoElasticsearchQuery;
    private final ServiceQuery serviceQuery;
    @Value("${elasticsearch.query.batch-size}")
    private final Integer batchSize;

    @Autowired
    public DetectSimilarityJobFactory(ServiceIndex serviceIndex, RepoSimilarityReport repoSimilarityReport, RepoElasticsearchQuery repoElasticsearchQuery, ServiceQuery serviceQuery, Integer batchSize) {
        this.serviceIndex = serviceIndex;
        this.repoSimilarityReport = repoSimilarityReport;
        this.repoElasticsearchQuery = repoElasticsearchQuery;
        this.serviceQuery = serviceQuery;
        this.batchSize = batchSize;
    }

    public DetectSimilarityJob newInstance(DetectSimilarityJob.DetectSimilarityJobBuilder builder) {
        return builder.serviceIndex(serviceIndex)
                      .serviceQuery(serviceQuery)
                      .batchSize(batchSize)

                      .repoSimilarityReport(repoSimilarityReport)
                      .repoElasticsearchQuery(repoElasticsearchQuery)
                      .build();
    }
}
