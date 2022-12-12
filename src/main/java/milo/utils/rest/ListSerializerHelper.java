package milo.utils.rest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ListSerializerHelper {

	private static final String listDelimiter = "@##@";

	public static List<String> deserialize(String value) {
		return deserialize(value, listDelimiter);
	}

	public static List<String> deserialize(String value, String delimiter) {
		return value == null ? Collections.emptyList() : Arrays.stream(value.split(delimiter))
				.filter(place -> !place.trim().isEmpty()).collect(Collectors.toList());
	}

	public static String serialize(List<String> values) {
		return serialize(values, listDelimiter);
	}

	public static String serialize(List<String> values, String delimiter) {
		return values == null ? null : values.stream().collect(Collectors.joining(delimiter));
	}
}
