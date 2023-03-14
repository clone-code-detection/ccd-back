package github.clone_code_detection.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Collection;

@Data
public class LanguageInfo {
    @JsonProperty("lang")
    private String language;
    @JsonProperty("index")
    private String index;
    @JsonProperty("ext")
    private Collection<String> extensions;
}
