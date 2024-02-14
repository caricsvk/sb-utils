package milo.utils.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SerializerHelper {

	private static final String listDelimiter = "@@#=";
	private static final String listDelimiterSubstitute = "@@listDelimiterSubstitute=#";

	public static List<String> stringToList(String value) {
		return stringToList(value, listDelimiter);
	}

	public static List<String> stringToList(String value, String delimiter) {
		return value == null ? new ArrayList<>() : Arrays.stream(value.split(delimiter))
				.map(item -> item.replaceAll(listDelimiterSubstitute, delimiter))
				.collect(Collectors.toCollection(ArrayList::new));
	}

	public static String listToString(List<String> values) {
		return listToString(values, listDelimiter);
	}

	public static String listToString(List<String> values, String delimiter) {
		return values == null ? null : values.stream()
				.map(value -> value == null ? "" : value)
				.map(value -> value.contains(delimiter) ? value.replaceAll(delimiter, listDelimiterSubstitute) : value)
				.collect(Collectors.joining(delimiter));
	}

	public static Map<String, String> stringToMap(String value) {
		return stringToMap(value, listDelimiter);
	}

	public static Map<String, String> stringToMap(String value, String delimiter) {
		Map<String, String> result = new HashMap<>();
		if (value != null && !value.isEmpty()) {
			Arrays.stream(value.split(delimiter))
					.map(item -> item.replaceAll(listDelimiterSubstitute, delimiter))
					.forEach(item -> {
						String[] keyValue = item.split(":");
						result.put(keyValue[0], keyValue.length > 1 ? keyValue[1] : null);
					});
		}
		return result;
	}

	public static String mapToString(Map<String, String> values) {
		return mapToString(values, listDelimiter);
	}

	public static String mapToString(Map<String, String> values, String delimiter) {
		return values == null ? null : values.entrySet()
				.stream().map(entry -> entry.getKey() + ":" + entry.getValue())
//				.map(value -> value.contains(delimiter) ? value.replaceAll(delimiter, listDelimiterSubstitute) : value)
				.collect(Collectors.joining(delimiter));
	}
}
