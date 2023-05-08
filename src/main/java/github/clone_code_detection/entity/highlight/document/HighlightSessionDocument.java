package github.clone_code_detection.entity.highlight.document;

import com.fasterxml.jackson.annotation.JsonProperty;
import github.clone_code_detection.entity.authenication.UserImpl;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Time;
import java.time.LocalTime;
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
    @Temporal(TemporalType.TIME)
    private Time created = Time.valueOf(LocalTime.now());

    @Column(name = "main_language")
    private String mainLanguage;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserImpl user;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    @JoinColumn(name = "session_id", referencedColumnName = "id")
    private Collection<HighlightSingleDocument> matches = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private HighlightSessionStatus status = HighlightSessionStatus.PENDING;

    public interface HighlightSessionProjection {
        UUID getId();

        String getName();

        Time getCreated();
        @JsonProperty("main_language")
        String getMainLanguage();
    }
}
