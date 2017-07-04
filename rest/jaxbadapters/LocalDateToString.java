package whitestein.shiftplanner.utils.rest.jaxbadapters;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class LocalDateToString extends XmlAdapter<String, LocalDate> {

	@Override
	public String marshal(LocalDate localDate) throws Exception {
		return localDate.format(DateTimeFormatter.ISO_DATE);
	}

	@Override
	public LocalDate unmarshal(String isoDate) throws Exception {
		return LocalDate.parse(isoDate);
	}

}
