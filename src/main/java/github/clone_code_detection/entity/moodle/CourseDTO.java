package github.clone_code_detection.entity.moodle;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseDTO {
    private long id;
    private String shortname;
    private String fullname;
    @JsonProperty("enrolled_user_count")
    private long enrolledUserCount;
    @JsonProperty("start_date")
    private ZonedDateTime startDate;
    @JsonProperty("end_date")
    private ZonedDateTime endDate;
    private boolean completed;

    List<AssignDTO> assigns;
}
