package milo.utils.eventstore.models;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EventHandlers<T extends Aggregate<T>> {

	private static Map<Class, Handler<?, ?>> eventsHandlers = new ConcurrentHashMap<>();

//	private Class<T> classType;
//
//	public EventHandlers(Class<T> classType) {
//		this.classType = classType;
//	}

	public <E extends Event> void register(Class<E> eventClass, Handler<T, E> handler) {
		if (!eventsHandlers.containsKey(eventClass)) {
			eventsHandlers.put(eventClass, handler);
		}
	}

	public T process(T aggregate, List<Event> events) {
//		T aggregate = null;
//		try {
//			aggregate = classType.newInstance();
//		} catch (InstantiationException | IllegalAccessException e) {
//			e.printStackTrace();
//		}
		for (Event event : events) {
			Handler handler = eventsHandlers.get(event.getClass());
			if (handler != null) {
				aggregate = (T) handler.handle(aggregate, event);
			}
		}
		return aggregate;
	}

	public interface Handler<T, E extends Event> {
		T handle(T aggregate, E event);
	}

}
