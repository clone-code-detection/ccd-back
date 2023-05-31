package github.clone_code_detection.entity.highlight.document;

import github.clone_code_detection.entity.authenication.UserImpl;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

@Entity
@Table(name = "highlight_session", schema = "highlight")
public class HighlightSessionDocument {
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name")
    private String name;

    @Builder.Default
    @Column(name = "created_time")
    @Temporal(TemporalType.TIMESTAMP)
    private ZonedDateTime created = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));

    @Column(name = "updated_at")
    @Builder.Default
    @Temporal(TemporalType.TIMESTAMP)
    private ZonedDateTime updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserImpl user;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    @JoinColumn(name = "session_id", referencedColumnName = "id")
    private Collection<HighlightSingleDocument> matches = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private HighlightSessionStatus status = HighlightSessionStatus.INIT;

    @Column(name = "exception", columnDefinition = "TEXT")
    private String exception;

    public interface HighlightSessionProjection {
        UUID getId();

        String getName();

        LocalDateTime getCreated();

        HighlightSessionStatus getStatus();

        Collection<HighlightSingleDocument> getMatches();
    }
}
