package github.clone_code_detection.repo.intra_project;

import github.clone_code_detection.entity.highlight_intra.document.dao.IntraProjectReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.UUID;

@Repository
public interface RepoIntraProjectReport extends JpaRepository<IntraProjectReport, UUID> {
    Collection<IntraProjectReport> getAllByUserId(UUID userId);

    @Query(
            value = """
                    select es_name
                    from highlight_intra.intra_project_report
                    where id = (select r.id
                                from highlight_intra.intra_project_report r
                                         join highlight_intra.author_report a on r.id = a.project_id
                                         join highlight.report_source_document d on a.id = d.report_id
                                where d.id = ?1);
                    """,
            nativeQuery = true
    )
    String getEsIndexBySourceId(UUID sourceId);
}
