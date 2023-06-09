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
    @JsonProperty("cm_id")
    private long cmId;
    private String name;
    @JsonProperty("due_date")
    private long dueDate;
    private String intro; // WARNING: this intro is the html element as string. Ex: "<p dir=\"ltr\" style=\"text-align: left;\">Add zip file for highlighting&nbsp;</p>"

    public static List<Assign> from(JsonNode moodleAssigns) {
        List<Assign> assigns = new ArrayList<>();
        moodleAssigns.forEach(moodleAssign -> assigns.add(Assign.builder()
                                                                .id(moodleAssign.get("id").asLong())
                                                                .cmId(moodleAssign.get("cmid").asLong())
                                                                .dueDate(moodleAssign.get("duedate").asLong())
                                                                .name(moodleAssign.get("name").asText())
                                                                .intro(moodleAssign.get("intro").asText())
                                                                .build()));
        return assigns;
    }
}
