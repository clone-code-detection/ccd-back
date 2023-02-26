package github.clone_code_detection.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import javax.annotation.Nullable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponseUnified<T> {
    @JsonProperty("message")
    @NonNull
    private String message;

    @JsonProperty("code")
    @NonNull
    private Integer code;

    @JsonProperty("data")
    @Nullable
    private T data;
}
