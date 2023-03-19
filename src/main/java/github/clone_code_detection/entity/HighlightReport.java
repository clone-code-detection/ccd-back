package github.clone_code_detection.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;
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
    @Column(columnDefinition = "nvarchar(255) default ''")
    String organization;
    @Column(columnDefinition = "nvarchar(255) default ''")
    String course;
    @Column(columnDefinition = "int not null default 0")
    int year;
    @Column(columnDefinition = "int not null default 1")
    int semester;
    @Column(columnDefinition = "nvarchar(255) default ''")
    String author;
    @Column(columnDefinition = "nvarchar(255) default ''")
    String project;
    @Column(columnDefinition = "nvarchar(255) default ''")
    String uri;
    @ElementCollection
    @CollectionTable(name = "highlight_report_extra_data")
    @Column(name = "extra_data", columnDefinition = "text not null")
    Set<String> extraData;
    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "datetime not null default now()", nullable = false)
    ZonedDateTime createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at", columnDefinition = "datetime not null default now() on update now()", nullable = false)
    ZonedDateTime updatedAt;
    @Column(name = "assigner", nullable = false)
    String assigner;
}
