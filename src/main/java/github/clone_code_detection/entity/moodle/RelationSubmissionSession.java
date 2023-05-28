package github.clone_code_detection.entity.moodle;

import github.clone_code_detection.entity.highlight.document.HighlightSessionDocument;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @OneToOne(cascade = {CascadeType.PERSIST})
    @JoinColumn(name = "session_id", referencedColumnName = "id")
    private HighlightSessionDocument session;
}
