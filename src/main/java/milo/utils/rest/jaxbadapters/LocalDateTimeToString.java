package milo.utils.rest.jaxbadapters;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class LocalDateTimeToString extends XmlAdapter<String, LocalDateTime> {

	@Override
	public String marshal(LocalDateTime localDateTime) throws Exception {
		String result = localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
		int dotIndex = result.indexOf('.');
		return dotIndex >= 0 ? result.substring(0, dotIndex) : result; // don't return milliseconds
	}

	@Override
	public LocalDateTime unmarshal(String isoDateTime) throws Exception {
		return LocalDateTime.parse(isoDateTime);
	}

}
