package milo.utils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class DateTimeHelper {

	public static LocalDateTime toLocalDateTime(Timestamp timestamp) {
		if (timestamp == null) {
			return null;
		}
		return LocalDateTime.ofEpochSecond(timestamp.getTime() / 1000, timestamp.getNanos(), ZoneOffset.UTC);
	}

	public static Timestamp toTimestamp(LocalDateTime localDateTime) {
		if (localDateTime == null) {
			return null;
		}
		Timestamp timestamp = new Timestamp(localDateTime.toEpochSecond(ZoneOffset.UTC) * 1000);
		timestamp.setNanos(localDateTime.getNano());
		return timestamp;
	}
}
