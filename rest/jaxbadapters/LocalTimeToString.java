package milo.utils.rest.jaxbadapters;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class LocalTimeToString extends XmlAdapter<String, LocalTime> {

	@Override
	public String marshal(LocalTime localTime) throws Exception {
		return localTime.format(DateTimeFormatter.ISO_TIME);
	}

	@Override
	public LocalTime unmarshal(String isoTime) throws Exception {
		return LocalTime.parse(isoTime);
	}

}
