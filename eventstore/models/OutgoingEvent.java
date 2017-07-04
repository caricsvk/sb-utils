package milo.utils.eventstore.models;

import java.time.LocalDateTime;

/**
 * Created by caric on 2/15/15.
 */
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