package milo.utils.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SerializerHelper {

	private static final String listDelimiter = "@@#=";
	private static final String listDelimiterSubstitute = "@@listDelimiterSubstitute=#";

	public static List<String> stringToList(String value) {
		return stringToList(value, listDelimiter);
	}

	public static List<String> stringToList(String value, String delimiter) {
		return value == null ? new ArrayList<>() : Arrays.stream(value.split(delimiter))
				.map(item -> item.replaceAll(listDelimiterSubstitute, listDelimiter))
				.collect(Collectors.toCollection(ArrayList::new));
	}

	public static String listToString(List<String> values) {
		return listToString(values, listDelimiter);
	}

	public static String listToString(List<String> values, String delimiter) {
		return values == null ? null : values.stream().map(
					value -> value != null && value.contains(listDelimiter) ?
						value.replaceAll(listDelimiter, listDelimiterSubstitute) : value
				).collect(Collectors.joining(delimiter));
	}
}
