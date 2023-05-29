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
public class AssignDTO {
    private long id;
    @JsonProperty("cm_id")
    private long cmId;
    private String name;
    @JsonProperty("due_date")
    private ZonedDateTime dueDate;
    private String intro; // WARNING: this intro is the html element as string. Ex: "<p dir=\"ltr\" style=\"text-align: left;\">Add zip file for highlighting&nbsp;</p>"

    List<Submission.SubmissionDTO> submissions;
}
