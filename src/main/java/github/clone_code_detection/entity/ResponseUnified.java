package github.clone_code_detection.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import javax.annotation.Nullable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponseUnified<T> {
    @JsonProperty(value = "message", required = true)
    private String message;

    @JsonProperty(value = "code", required = true)
    private Integer code = 0;

    @JsonProperty("data")
    @Nullable
    private T data;
}
