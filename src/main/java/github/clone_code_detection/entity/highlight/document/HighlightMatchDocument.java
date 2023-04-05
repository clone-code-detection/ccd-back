package github.clone_code_detection.entity.highlight.document;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data

@Entity
@Table(name = "highlight_single_document_match", schema = "highlight")
public class HighlightMatchDocument {
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "start_offset")
    private Integer start;

    @Column(name = "end_offset")
    private Integer end;
}
