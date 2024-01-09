package milo.utils.eventstore.models;

import milo.utils.rest.jaxbadapters.LocalDateTimeToString;

import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.LocalDateTime;

public abstract class OutgoingEvent implements Event {

	protected Class<?> aggregateType;
	private LocalDateTime created;

	public <T extends Aggregate<T>> OutgoingEvent(Class<T> classType) {
		aggregateType = classType;
		created = LocalDateTime.now();
	}

	@Override
	public String getAggregateName() {
		return aggregateType.getSimpleName();
	}

	@Override
	public String getEventName() {
		return this.getClass().getSimpleName();
	}

	@Override
	@XmlJavaTypeAdapter(LocalDateTimeToString.class)
	public LocalDateTime getCreated() {
		if (created == null) {
			created = LocalDateTime.now();
		}
		return created;
	}

	public void setCreated(LocalDateTime created) {
		this.created = created;
	}
}
