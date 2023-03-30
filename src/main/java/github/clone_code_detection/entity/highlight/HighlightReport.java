package github.clone_code_detection.entity.highlight;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity(name = "highlight_report")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "highlight_report", indexes = @Index(name = "report_unique_id", columnList = "organization, year, semester, course, assigner, project, author", unique = true))
public class HighlightReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;
    @Column(nullable = false)
    @Builder.Default
    String organization = "test";
    @Column(nullable = false)
    @Builder.Default
    String course = "test";
    @Column(nullable = false)
    @Builder.Default
    int year = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).getYear();
    @Column(nullable = false)
    @Builder.Default
    int semester = 1;
    @Column(nullable = false)
    @Builder.Default
    String author = "participant";
    @Column(nullable = false)
    @Builder.Default
    String project = "test";
    @Column(nullable = false)
    String uri = "";
    @ElementCollection
    @CollectionTable(name = "highlight_report_extra_data")
    @Column(name = "extra_data")
    @Builder.Default
    Set<String> extraData = new HashSet<>();
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    ZonedDateTime createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    ZonedDateTime updatedAt;
    @Column(name = "assigner", nullable = false)
    @Builder.Default
    String assigner = "admin";
}
