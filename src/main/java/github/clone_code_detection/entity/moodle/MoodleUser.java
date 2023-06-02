package github.clone_code_detection.entity.moodle;

import com.fasterxml.jackson.databind.JsonNode;
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
@Table(name = "user", schema = "moodle")
public class MoodleUser {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "fullname")
    private String fullname;

    private String email;

    @Column(name = "reference_user_id", unique = true)
    private long referenceUserId;

    @Column(name = "avatar")
    private String profileImageUrl;

    public static MoodleUser from(JsonNode account) {
        return MoodleUser.builder()
                         .email(account.get("email").asText())
                         .fullname(account.get("fullname").asText())
                         .referenceUserId(account.get("id").asLong())
                         .profileImageUrl(account.get("profileimageurlsmall").asText())
                         .build();
    }

    public MoodleUserDTO toDTO() {
        return new MoodleUserDTO() {
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

    public interface MoodleUserDTO {
        String getFullname();

        String getEmail();

        String getProfileImageUrl();
    }
}
