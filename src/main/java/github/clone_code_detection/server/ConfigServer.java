package github.clone_code_detection.server;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//TODO: Implement config util to read from multiple profiles
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConfigServer {
    private Integer port = 8080;
}
