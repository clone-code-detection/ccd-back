package github.clone_code_detection.util;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

public class ModelMapperUtil {
    private static final ModelMapper mapper = new ModelMapper() {{
        getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
    }};

    public static ModelMapper getMapper() {
        return mapper;
    }
}
