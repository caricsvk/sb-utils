package milo.utils.rest.jaxbadapters;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.sql.Time;

public class TimeToString extends XmlAdapter<String, Time> {

	@Override
	public String marshal(Time time) throws Exception {
		return time == null ? null : time.toString().length() >= 8 ? time.toString().substring(0, 5) : time.toString();
	}

	@Override
	public Time unmarshal(String time) throws Exception {
		if (time != null && time.length() == 5) {
			time += ":00";
		}
		return Time.valueOf(time);
	}

}
