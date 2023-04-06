package milo.utils;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class DateTimeHelper {

	public static LocalDateTime toLocalDateTime(Timestamp timestamp) {
		if (timestamp == null) {
			return null;
		}
		return timestamp.toLocalDateTime();
	}

	public static ZonedDateTime toZonedDateTime(Timestamp timestamp) {
		if (timestamp == null) {
			return null;
		}
		return ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp.getTime()), ZoneId.systemDefault());
	}

	public static Timestamp toTimestamp(LocalDateTime localDateTime) {
		if (localDateTime == null) {
			return null;
		}
		return toTimestamp(localDateTime.atZone(ZoneId.systemDefault()));
	}

	public static Timestamp toTimestamp(ZonedDateTime zonedDateTime) {
		if (zonedDateTime == null) {
			return null;
		}
		return Timestamp.from(zonedDateTime.toInstant());
	}
}
