package milo.utils.rest.jaxbadapters;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeToString extends XmlAdapter<String, LocalDateTime> {

	@Override
	public String marshal(LocalDateTime localDateTime) throws Exception {
		return localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
	}

	@Override
	public LocalDateTime unmarshal(String isoDateTime) throws Exception {
		return LocalDateTime.parse(isoDateTime);
	}

}