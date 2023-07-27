package github.clone_code_detection.entity.highlight_intra.document;

import github.clone_code_detection.entity.authenication.UserImpl;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A project contains one or more author
 */

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "intra_project_report", schema = "highlight_intra")
public class IntraProjectReport {
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name")
    @Builder.Default
    private String name = "no name";

    @Column(name = "note")
    @Builder.Default
    private String note = "";

    @Column(name = "status")
    @Builder.Default
    private String status = "created";

    @Column(name = "es_name")
    private String esIndex;


    @Column(name = "created_at")
    @Builder.Default
    @Temporal(TemporalType.TIMESTAMP)
    private ZonedDateTime createdAt = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));

    @Column(name = "updated_at")
    @Builder.Default
    @Temporal(TemporalType.TIMESTAMP)
    private ZonedDateTime updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserImpl user;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
    @JoinColumn(name = "project_id", referencedColumnName = "id")
    @Fetch(FetchMode.SUBSELECT)
    @Builder.Default
    private List<AuthorReport> authorReports = new ArrayList<>();

    public void addAuthorReport(AuthorReport authorReport) {
        this.authorReports.add(authorReport);
    }
}
