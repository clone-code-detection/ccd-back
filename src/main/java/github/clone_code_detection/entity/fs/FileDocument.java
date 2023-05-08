package github.clone_code_detection.entity.fs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import github.clone_code_detection.entity.authenication.UserImpl;
import jakarta.persistence.*;
import lombok.*;

import java.nio.charset.StandardCharsets;
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

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private UserImpl user;

    @Builder.Default
    @Column(name = "author")
    private String author = "anonymous";

    @JsonProperty("content")
    public String getContentAsString() {
        return new String(this.content, StandardCharsets.UTF_8);
    }
}
