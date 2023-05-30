package github.clone_code_detection.entity.moodle;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "submission_file", schema = "moodle")
public class SubmissionFile {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "file_uri")
    private String fileUri;

    @Column(name = "filename")
    private String filename;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @Column(name = "mimetype")
    private String mimetype;
}
