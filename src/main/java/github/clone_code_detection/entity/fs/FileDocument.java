package github.clone_code_detection.entity.fs;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "file", schema = "file")
public class FileDocument {
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "content", nullable = false)
    @Getter(AccessLevel.NONE)
    private byte[] content;

    @JsonProperty("file_name")
    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Builder.Default
    @Column(name = "author")
    private String author = "anonymous";

    private String origin;

    @Column(name = "origin_link")
    private String originLink;

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "key")
    @Column(name = "value")
    @CollectionTable(schema = "file",
                     name = "file_meta",
                     joinColumns = @JoinColumn(name = "file_id", referencedColumnName = "id"))
    private Map<String, String> meta;

    @JsonProperty("content")
    public String getContentAsString() {
        return new String(this.content, StandardCharsets.UTF_8);
    }
}
