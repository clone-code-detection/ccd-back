package github.clone_code_detection.entity.highlight.document;

import github.clone_code_detection.entity.authenication.UserImpl;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
@Table(name = "similarity_report", schema = "highlight")
public class SimilarityReport {
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

    @OneToOne(cascade = {CascadeType.ALL}, fetch = FetchType.LAZY)
    private SimilarityReportMeta meta;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE}, fetch = FetchType.EAGER)
    @JoinColumn(name = "report_id", referencedColumnName = "id")
    private Collection<ReportSourceDocument> sources = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private SimilarityReportStatus status = SimilarityReportStatus.INIT;

    @Column(name = "exception", columnDefinition = "TEXT")
    private String exception;

    public interface SimilarityReportDTO {
        UUID getId();

        String getName();

        ZonedDateTime getCreated();

        SimilarityReportStatus getStatus();

        Collection<ReportSourceDocument> getSources();
    }
}
