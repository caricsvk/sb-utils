package milo.utils.eventstore.models;

import java.time.LocalDateTime;

/**
 * Created by caric on 2/15/15.
 */
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
	public LocalDateTime getCreated() {
		return created;
	}

	public void setCreated(LocalDateTime created) {
		this.created = created;
	}
}