package whitestein.shiftplanner.utils.jpa;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class DateTimeFactory {

	public static LocalDateTime toLocalDateTime(Timestamp timestamp) {
		if (timestamp == null) {
			return null;
		}
		return LocalDateTime.ofEpochSecond(timestamp.getTime() / 1000, timestamp.getNanos(), ZoneOffset.UTC);
	}

	public static ZonedDateTime toZonedDateTime(Timestamp timestamp) {
		if (timestamp == null) {
			return null;
		}
		return ZonedDateTime.of(toLocalDateTime(timestamp), ZoneOffset.systemDefault());
	}

	public static Timestamp toTimestamp(LocalDateTime localDateTime) {
		if (localDateTime == null) {
			return null;
		}
		return toTimestamp(localDateTime.toEpochSecond(ZoneOffset.UTC), localDateTime.getNano());
	}

	public static Timestamp toTimestamp(ZonedDateTime zonedDateTime) {
		if (zonedDateTime == null) {
			return null;
		}
		return toTimestamp(zonedDateTime.toEpochSecond(), zonedDateTime.getNano());
	}

	private static Timestamp toTimestamp(long epochSeconds, int nano) {
		Timestamp timestamp = new Timestamp(epochSeconds * 1000);
		timestamp.setNanos(nano);
		return timestamp;
	}
}
