package milo.utils.jpa;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class DateTimeFactory {

	public static ZonedDateTime toZonedDateTime(Timestamp timestamp) {
		if (timestamp == null) {
			return null;
		}
		return ZonedDateTime.of(timestamp.toLocalDateTime(), ZoneOffset.systemDefault());
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
