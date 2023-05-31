package github.clone_code_detection.entity.moodle;

import github.clone_code_detection.entity.highlight.document.HighlightSessionDocument;
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
@Table(name = "relation_submission_session", schema = "moodle")
public class RelationSubmissionSession {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @OneToOne(cascade = {CascadeType.REMOVE})
    @JoinColumn(name = "session_id", referencedColumnName = "id")
    private HighlightSessionDocument session;

    @OneToOne(cascade = {CascadeType.ALL})
    @JoinColumn(name = "submission_file_id", referencedColumnName = "id")
    private SubmissionFile file;

    private enum Status {
        UN_DETECTED, UP_TO_DATE, OUT_DATE
    }

    public RelationSubmissionSessionDTO toDTO() {
        return new RelationSubmissionSessionDTO() {
            @Override
            public SubmissionFile getFile() {
                return file;
            }

            @Override
            public UUID getSessionId() {
                if (session != null) return session.getId();
                return null;
            }

            @Override
            public Status getStatus() {
                if (session == null)
                    return Status.UN_DETECTED;
                if (file.getUpdatedAt().isBefore(session.getUpdatedAt()))
                    return Status.UP_TO_DATE;
                return Status.OUT_DATE;
            }
        };
    }

    public interface RelationSubmissionSessionDTO {
        SubmissionFile getFile();

        UUID getSessionId();
        Status getStatus();
    }
}
