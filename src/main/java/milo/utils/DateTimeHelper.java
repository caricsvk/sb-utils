package milo.utils;

import java.sql.Timestamp;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

public class DateTimeHelper {

	static final List<DateTimeFormatter> dateTimeFormatters = Arrays.asList(
			DateTimeFormatter.ISO_OFFSET_DATE_TIME,
			DateTimeFormatter.RFC_1123_DATE_TIME,
			DateTimeFormatter.ISO_DATE_TIME,
			DateTimeFormatter.ISO_LOCAL_DATE_TIME,
			DateTimeFormatter.ISO_INSTANT,
			DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX")
	);

	static final List<DateTimeFormatter> dateFormatters = Arrays.asList(
			DateTimeFormatter.ISO_OFFSET_DATE,
			DateTimeFormatter.ISO_DATE,
			DateTimeFormatter.ISO_LOCAL_DATE
	);

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

	public static LocalDateTime parseLocalDateTime(String dateTimeString) {
		for (DateTimeFormatter dateTimeFormatter : dateTimeFormatters) {
			try {
				return LocalDateTime.from(dateTimeFormatter.parse(dateTimeString));
			} catch (DateTimeException exception) {
//				System.out.println("BaseResource.getDate caught ex 1 for: '" + dateTimeString + "'");
			}
		}
		for (DateTimeFormatter dateFormatter : dateFormatters) {
			try {
				return LocalDate.from(dateFormatter.parse(dateTimeString)).atStartOfDay();
			} catch (DateTimeParseException exception) {
//				System.out.println("BaseResource.getDate caught ex 2 for: '" + dateTimeString + "'");
			}
		}
//		System.out.println("BaseResource.getDate warning date formatter not found for " + dateTimeString);
		return LocalDateTime.now();
	}

	public static ZonedDateTime parseZonedDateTime(String dateTimeString) {
		return parseZonedDateTime(dateTimeString, ZoneId.systemDefault());
	}

	public static ZonedDateTime parseZonedDateTime(String dateTimeString, ZoneId defaultZoneId) {
		for (DateTimeFormatter dateTimeFormatter : dateTimeFormatters) {
			try {
				return ZonedDateTime.from(dateTimeFormatter.parse(dateTimeString));
			} catch (DateTimeException exception) {
//				LOG.warning("BaseResource.getDate caught ex 1 for: '" + dateTimeString + "' " + exception.getMessage());
			}
		}
		for (DateTimeFormatter dateFormatter : dateFormatters) {
			try {
				return LocalDate.from(dateFormatter.parse(dateTimeString)).atStartOfDay().atZone(defaultZoneId);
			} catch (DateTimeParseException exception) {
//				System.out.println("BaseResource.getDate caught ex 2 for: '" + dateTimeString + "'");
			}
		}
//		System.out.println("BaseResource.getDate warning date formatter not found for " + dateTimeString);
		return ZonedDateTime.now();
	}

}
