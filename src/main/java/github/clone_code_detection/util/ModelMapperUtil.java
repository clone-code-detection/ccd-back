package github.clone_code_detection.util;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

public class ModelMapperUtil {
    private ModelMapperUtil() {
        throw new IllegalStateException("ModelMapperUtil is utility class");
    }
    private static final ModelMapper mapper =  new ModelMapper();

    static {
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
    }

    public static ModelMapper getMapper() {
        return mapper;
    }
}
