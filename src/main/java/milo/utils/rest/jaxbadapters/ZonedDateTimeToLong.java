package milo.utils.rest.jaxbadapters;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class ZonedDateTimeToLong extends XmlAdapter<Long, ZonedDateTime> {

	@Override
	public Long marshal(ZonedDateTime timestamp) throws Exception {
		return timestamp.toInstant().toEpochMilli();
	}

	@Override
	public ZonedDateTime unmarshal(Long timestamp) throws Exception {
		return ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
	}

}
