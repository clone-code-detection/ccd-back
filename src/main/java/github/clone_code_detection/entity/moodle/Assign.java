package github.clone_code_detection.entity.moodle;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Assign {
    private long id;
    private String name;
    @JsonProperty("due_date")
    private long dueDate;
    private String intro; // WARNING: this intro is the html element as string. Ex: "<p dir=\"ltr\" style=\"text-align: left;\">Add zip file for highlighting&nbsp;</p>"
    private long courseId;
    private String courseName;

    public static List<Assign> from(JsonNode moodleCourse) {
        List<Assign> assigns = new ArrayList<>();
        long courseId = moodleCourse.get("id").asLong();
        String courseName = moodleCourse.get("fullname").asText();
        moodleCourse.get("assignments").forEach(moodleAssign -> assigns.add(Assign.builder()
                                                                                  .id(moodleAssign.get("id").asLong())
                                                                                  .dueDate(moodleAssign.get("duedate")
                                                                                                       .asLong())
                                                                                  .name(moodleAssign.get("name")
                                                                                                    .asText())
                                                                                  .intro(moodleAssign.get("intro")
                                                                                                     .asText())
                                                                                  .courseId(courseId)
                                                                                  .courseName(courseName)
                                                                                  .build()));
        return assigns;
    }
}
