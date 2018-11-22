package milo.utils.eventstore;

import milo.utils.eventstore.models.Event;

public interface MessageBroker {

	void publish(Event event) throws Exception;

	<T extends Event> void subscribe(Class<T> eventType, EventHandler<T> eventHandler) throws Exception;

}
