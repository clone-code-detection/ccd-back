package github.clone_code_detection.entity.moodle;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "submission_owner", schema = "moodle")
public class SubmissionOwner {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "fullname")
    private String fullname;

    private String email;

    private long referenceOwnerId;

    @Column(name = "avatar")
    private String profileImageUrl;

    public SubmissionOwnerDTO toDTO() {
        return new SubmissionOwnerDTO() {
            @Override
            public String getFullname() {
                return fullname;
            }

            @Override
            public String getEmail() {
                return email;
            }

            @Override
            public String getProfileImageUrl() {
                return profileImageUrl;
            }
        };
    }

    public interface SubmissionOwnerDTO {
        String getFullname();

        String getEmail();

        String getProfileImageUrl();
    }
}
