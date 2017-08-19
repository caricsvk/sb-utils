package milo.utils.rest.jaxbadapters;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class DurationToSeconds extends XmlAdapter<Long, Duration> {

	@Override
	public Long marshal(Duration duration) throws Exception {
		if (duration == null) {
			return null;
		}
		return duration.getSeconds();
	}

	@Override
	public Duration unmarshal(Long durationLong) throws Exception {
		if (durationLong == null) {
			return null;
		}
		return Duration.of(durationLong, ChronoUnit.SECONDS);
	}

}
