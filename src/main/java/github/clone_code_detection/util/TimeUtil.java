package github.clone_code_detection.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class TimeUtil {
    static final ZoneId ZONE_ID = ZoneId.of("Asia/Ho_Chi_Minh");

    private TimeUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static ZonedDateTime parseZoneDateTime(long timestamp) {
        return Instant.ofEpochSecond(timestamp).atZone(ZONE_ID);
    }
}
