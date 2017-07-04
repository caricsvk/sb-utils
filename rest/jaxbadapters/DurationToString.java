package whitestein.shiftplanner.utils.rest.jaxbadapters;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class DurationToString extends XmlAdapter<String, Duration> {

	@Override
	public String marshal(Duration duration) throws Exception {
		if (duration == null) {
			return null;
		}
		long hours = duration.toHours();
		long minutes = duration.minusHours(hours).toMinutes();
		long seconds = duration.minusHours(hours).minusMinutes(minutes).get(ChronoUnit.SECONDS);
		return hours + ":" + minutes + ":" + seconds;
	}

	@Override
	public Duration unmarshal(String durationStr) throws Exception {
		if (durationStr == null || durationStr.isEmpty()) {
			return null;
		}
		Duration duration = Duration.ZERO;
		String[] durationArrays = durationStr.split(":");
		duration.plusHours(Integer.valueOf(durationArrays[0]));
		duration.plusMinutes(Integer.valueOf(durationArrays[1]));
		duration.plusSeconds(Integer.valueOf(durationArrays[2]));
		return duration;
	}

}
