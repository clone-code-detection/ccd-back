package github.clone_code_detection.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtil {
    private JsonUtil() {
        throw new IllegalStateException("JsonUtil is utility class");
    }
    private static final ObjectMapper mapper = new ObjectMapper();

    public static <T> T jsonFromString(String value, Class<T> clazz) throws JsonProcessingException {
            return mapper.readValue(value, clazz);
    }
}
