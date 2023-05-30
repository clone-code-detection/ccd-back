package github.clone_code_detection.entity.moodle;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.JsonArray;
import github.clone_code_detection.util.TimeUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.ArrayList;
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

    public static List<AssignDTO> from(JsonArray moodleAssigns) {
        List<AssignDTO> assigns = new ArrayList<>();
        moodleAssigns.forEach(moodleAssign -> assigns.add(AssignDTO.builder()
                                                                   .id(moodleAssign.getAsJsonObject()
                                                                                   .get("id")
                                                                                   .getAsLong())
                                                                   .cmId(moodleAssign.getAsJsonObject()
                                                                                     .get("cmid")
                                                                                     .getAsLong())
                                                                   .dueDate(TimeUtil.parseZoneDateTime(moodleAssign.getAsJsonObject()
                                                                                                                   .get("duedate")))
                                                                   .name(moodleAssign.getAsJsonObject()
                                                                                     .get("name")
                                                                                     .getAsString())
                                                                   .intro(moodleAssign.getAsJsonObject()
                                                                                      .get("intro")
                                                                                      .getAsString())
                                                                   .build()));
        return assigns;
    }
}
