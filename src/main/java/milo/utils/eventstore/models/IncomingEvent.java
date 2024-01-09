package milo.utils.eventstore.models;

import milo.utils.rest.jaxbadapters.LocalDateTimeToString;

import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.LocalDateTime;

public abstract class IncomingEvent implements Event {

	private LocalDateTime created;

	public IncomingEvent() {
		created = LocalDateTime.now();
	}

	@Override
	public String getEventName() {
		return this.getClass().getSimpleName();
	}

	@Override
	@XmlJavaTypeAdapter(LocalDateTimeToString.class)
	public LocalDateTime getCreated() {
		return created;
	}

	public void setCreated(LocalDateTime created) {
		this.created = created;
	}
}
