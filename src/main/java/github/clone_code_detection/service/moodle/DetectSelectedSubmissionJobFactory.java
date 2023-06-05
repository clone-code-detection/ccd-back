package github.clone_code_detection.service.moodle;

import github.clone_code_detection.repo.RepoElasticsearchDelete;
import github.clone_code_detection.repo.RepoSimilarityReport;
import github.clone_code_detection.repo.RepoSubmission;
import github.clone_code_detection.service.query.ServiceQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class DetectSelectedSubmissionJobFactory {
    private final RepoSimilarityReport repoSimilarityReport;
    private final RepoSubmission repoSubmission;
    private final ServiceQuery serviceQuery;
    private final RepoElasticsearchDelete repoElasticsearchDelete;
    private final RestTemplate moodleClient;

    @Autowired
    public DetectSelectedSubmissionJobFactory(RepoSimilarityReport repoSimilarityReport,
                                              RepoSubmission repoSubmission,
                                              @Lazy  ServiceQuery serviceQuery,
                                              RepoElasticsearchDelete repoElasticsearchDelete, RestTemplate moodleClient) {
        this.repoSimilarityReport = repoSimilarityReport;
        this.repoSubmission = repoSubmission;
        this.serviceQuery = serviceQuery;
        this.repoElasticsearchDelete = repoElasticsearchDelete;
        this.moodleClient = moodleClient;
    }

    public DetectSelectedSubmissionJob newInstance(DetectSelectedSubmissionJob.DetectSelectedSubmissionJobBuilder builder) {
        return builder
                .repoSimilarityReport(repoSimilarityReport)
                .repoSubmission(repoSubmission)
                .repoElasticsearchDelete(repoElasticsearchDelete)

                .serviceQuery(serviceQuery)
                .moodleClient(moodleClient)
                .build();

    }
}
