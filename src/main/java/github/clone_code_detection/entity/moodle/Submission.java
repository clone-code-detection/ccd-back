package github.clone_code_detection.entity.moodle;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "submission", schema = "moodle")
public class Submission {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "course_id")
    private long courseId;

    @Column(name = "course_name")
    private String courseName;

    @Column(name = "assign_id")
    private long assignId;

    @Column(name = "assign_name")
    private String assignName;

    @Column(name = "reference_submission_id")
    private long referenceSubmissionId; // The submission id of moodle

    @ManyToOne(cascade = {CascadeType.PERSIST})
    @JoinColumn(name = "owner_id", referencedColumnName = "id")
    private MoodleUser owner;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private java.time.ZonedDateTime createdAt = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));

    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    @Builder.Default
    private java.time.ZonedDateTime updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));

    @OneToMany(cascade = {CascadeType.ALL}, fetch = FetchType.EAGER)
    @JoinColumn(name = "submission_id", referencedColumnName = "id")
    private List<RelationSubmissionReport> relations;

    public SubmissionDTO toDTO() {
        return new SubmissionDTO() {
            @Override
            public long getId() {
                return id;
            }

            @Override
            public long getCreatedAt() {
                return createdAt.getLong(ChronoField.INSTANT_SECONDS);
            }

            @Override
            public long getUpdatedAt() {
                return updatedAt.getLong(ChronoField.INSTANT_SECONDS);
            }

            @Override
            public MoodleUser.MoodleUserDTO getOwner() {
                return owner.toDTO();
            }

            @Override
            public List<RelationSubmissionReport.RelationSubmissionSessionDTO> getRelations() {
                return relations.stream().map(RelationSubmissionReport::toDTO).toList();
            }
        };
    }

    public interface SubmissionDTO {
        long getId();

        long getCreatedAt();

        long getUpdatedAt();

        MoodleUser.MoodleUserDTO getOwner();

        List<RelationSubmissionReport.RelationSubmissionSessionDTO> getRelations();
    }
}
