package github.clone_code_detection.repo;

import github.clone_code_detection.entity.highlight.HighlightReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HighlightReportRepository extends JpaRepository<HighlightReport, Integer> {
    HighlightReport findHighlightReportByOrganizationAndYearAndSemesterAndCourseAndAssignerAndProjectAndAuthor(
            String organization, int year, int semester, String course, String assigner, String project, String author);
}
