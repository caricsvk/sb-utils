package milo.utils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class DateTimeHelper {

	public static LocalDateTime toLocalDateTime(Timestamp timestamp) {
		if (timestamp == null) {
			return null;
		}
		return timestamp.toLocalDateTime();
	}

	public static Timestamp toTimestamp(LocalDateTime localDateTime) {
		if (localDateTime == null) {
			return null;
		}
		return Timestamp.valueOf(localDateTime);
	}
}
