package github.clone_code_detection.entity.highlight.report;

import github.clone_code_detection.entity.authenication.UserImpl;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Time;
import java.util.Collection;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data

@Entity
@Table(name = "highlight_session", schema = "highlight")
public class HighlightSessionDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Column(name = "created_time")
    @Temporal(TemporalType.TIME)
    private Time created;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private UserImpl User;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    @JoinColumn(name = "session_id", referencedColumnName = "id")
    private Collection<HighlightSingleDocument> matches;
}
