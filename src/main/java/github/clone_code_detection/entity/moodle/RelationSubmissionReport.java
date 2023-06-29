package github.clone_code_detection.entity.moodle;

import github.clone_code_detection.entity.highlight.document.SimilarityReport;
import github.clone_code_detection.entity.highlight.document.SimilarityReportStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "relation_submission_report", schema = "moodle")
public class RelationSubmissionReport {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @OneToOne(cascade = {CascadeType.REMOVE})
    @JoinColumn(name = "report_id", referencedColumnName = "id")
    private SimilarityReport report;

    @OneToOne(cascade = {CascadeType.ALL})
    @JoinColumn(name = "submission_file_id", referencedColumnName = "id")
    private SubmissionFile file;

    public RelationSubmissionSessionDTO toDTO() {
        return new RelationSubmissionSessionDTO() {
            @Override
            public SubmissionFile getFile() {
                return file;
            }

            @Override
            public UUID getSessionId() {
                if (report != null) return report.getId();
                return null;
            }

            @Override
            public Status getStatus() {
                if (report == null) return Status.UN_DETECTED;
                if (file.getUpdatedAt().isBefore(report.getUpdatedAt()) && report.getStatus().equals(SimilarityReportStatus.DONE)) return Status.UP_TO_DATE;
                return Status.OUT_DATE;
            }
        };
    }

    private enum Status {
        UN_DETECTED, UP_TO_DATE, OUT_DATE
    }

    public interface RelationSubmissionSessionDTO {
        SubmissionFile getFile();

        UUID getSessionId();

        Status getStatus();
    }
}
