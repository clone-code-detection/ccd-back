package github.clone_code_detection.util;

import com.google.gson.JsonElement;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class TimeUtil {
    static final ZoneId ZONE_ID = ZoneId.of("Asia/Ho_Chi_Minh");

    private TimeUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static ZonedDateTime parseZoneDateTime(JsonElement timestamp) {
        return Instant.ofEpochSecond(timestamp.getAsInt()).atZone(ZONE_ID);
    }
}
