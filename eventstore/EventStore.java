package milo.utils.eventstore;

import milo.utils.eventstore.exceptions.AggregateAccessException;
import milo.utils.eventstore.models.Aggregate;
import milo.utils.eventstore.models.Event;
import milo.utils.eventstore.models.StoreObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class EventStore {

	private static final Logger LOG = Logger.getLogger(EventStore.class.getName());

	private final MessageBroker messageBroker;

	protected EventStore(MessageBroker messageBroker) {
		this.messageBroker = messageBroker;
	}

	protected abstract <T extends Aggregate<T>> void put(final StoreObject<T> storeObject);

	protected abstract <T extends Aggregate<T>> StoreObject getStoredObject(final Class<T> entityType, final String aggregateId);

	public <T extends Event> void subscribe(Class<T> eventType, EventHandler<T> eventHandler) {
		messageBroker.subscribe(eventType, eventHandler);
	}

	public <T extends Aggregate<T>> String create(final Class<T> aggregateType, final Event event) {
		List<Event> events = new ArrayList<>();
		events.add(event);
		return create(aggregateType, events);
	}

	public <T extends Aggregate<T>> String create(final Class<T> aggregateType, final List<Event> events) {
		UUID uuid = UUID.randomUUID();
		T entity = null;
		try {
			entity = aggregateType.newInstance().applyEvents(events);
			entity.setAggregateId(uuid.toString(), 0);
			read(entity.getClass(), uuid.toString());
		} catch (InstantiationException | IllegalAccessException ex) {
			throw new AggregateAccessException(aggregateType.getName()).setUnavailable();
		} catch (AggregateAccessException ex) { //aggregate id does not exists
			put(new StoreObject<>(entity, events));
			emitEvents(events);
			return entity.getAggregateId();
		}

		//try other id
		return create(aggregateType, events);
	}

	private void emitEvents(List<Event> events) {
		for (Event event : events)
			try {
				EventStore.logEvent("emit", event.getEventName(), null);
				messageBroker.send(event);
			} catch (Exception e) {
				LOG.log(Level.WARNING, e.getMessage(), e);
			}
	}

	public static void logEvent(String prefix, String eventName, String message) {
		if (LOG.isLoggable(Level.INFO)) {
			List<StackTraceElement> stackTraceElements = Arrays.asList(Thread.currentThread().getStackTrace());
			List<String> reducedStacks = Arrays.asList(new String[]{"sun", "jav", "com", "org", "net", "uti"});
			Optional<StackTraceElement> anyStack = stackTraceElements.stream().filter(element ->
					!reducedStacks.contains(element.toString().substring(0, 3))
							&& !element.toString().startsWith("milo.utils")).findAny();
			if (message != null && message.length() > 50) {
				message = message.replaceAll("\\w{30,}", ".. ");
			}
			LOG.log(Level.INFO, "EventStore " + prefix + " message =========== " + eventName
					+ " [" + anyStack + "] " + message);
		}
	}

	public <T extends Aggregate<T>> void update(final String id, final Class<T> aggregateClassType,
	                                            final List<Event> newEvents) {
		update(id, aggregateClassType, newEvents, null);
	}

	public <T extends Aggregate<T>> void update(final String id, final Class<T> aggregateClassType, final Event event) {
		update(id, aggregateClassType, event, null);
	}

	public <T extends Aggregate<T>> void update(final String id, final Class<T> aggregateClassType, final Event event,
	                                            final Integer version) {
		List<Event> events = new ArrayList<>();
		events.add(event);
		update(id, aggregateClassType, events, version);
	}

	public <T extends Aggregate<T>> void update(final String id, final Class<T> aggregateClassType,
	                                            final List<Event> newEvents, final Integer version) {

		final StoreObject<T> storedObject = getStoredObject(aggregateClassType, id);
		Integer storedObjectVersion = storedObject.getCurrentSnapshot().getAggregateVersion();
		//check version when is needed
		if (version != null && version != storedObjectVersion) {
			throw new AggregateAccessException("aggregate " + id + " [" + aggregateClassType.getName()
					+ ", v: " + version + "] has been already updated to version "
					+ storedObjectVersion).setConflict();
		}
		if (storedObjectVersion == null) {
			storedObjectVersion = -1;
		}
		storedObject.addEvents(newEvents);
		storedObject.getCurrentSnapshot().setAggregateId(id, storedObjectVersion + 1);
		put(storedObject);
		emitEvents(newEvents);
	}

	public <T extends Aggregate<T>> boolean exists(final Class<T> entityType, final String aggregateId) {
		try {
			getStoredObject(entityType, aggregateId);
			return true;
		} catch (AggregateAccessException ex) {
			return false;
		}
	}

	public <T extends Aggregate<T>> T read(final Class<T> entityType, final String aggregateId) {
		StoreObject<T> storedObject = getStoredObject(entityType, aggregateId);
		T currentSnapshot = storedObject.getCurrentSnapshot();
		Integer aggregateVersion = currentSnapshot.getAggregateVersion();
		if (storedObject.getCurrentEvents() != null) {
			currentSnapshot = currentSnapshot.applyEvents(storedObject.getCurrentEvents());
		}
		currentSnapshot.setAggregateId(aggregateId, aggregateVersion);
		return currentSnapshot;
	}

	public <T extends Aggregate<T>> void delete(final Class<T> entityType, final String aggregateId) {
		try {
			T aggregate = entityType.newInstance();
			aggregate.setAggregateId(aggregateId, null);
			put(new StoreObject<>(aggregate, null));
		} catch (InstantiationException | IllegalAccessException e) {
			LOG.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	public <T extends Aggregate<T>> List<Event> readEvents(final Class<T> entityType, final String aggregateId) {
		StoreObject<T> storedObject = getStoredObject(entityType, aggregateId);
		return storedObject.getCurrentEvents();
	}

	protected byte[] marshallObject(StoreObject storeObject) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		byte[] bytes = null;
		try {
			out = new ObjectOutputStream(bos);
			out.writeObject(storeObject);
			bytes = bos.toByteArray();
		} catch (IOException e) {
			LOG.log(Level.WARNING, e.getMessage(), e);
		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (IOException ex) {
				// ignore close exception
			}
			try {
				bos.close();
			} catch (IOException ex) {
				// ignore close exception
			}
		}
		return bytes;
	}

	protected <T> T unmarshallObject(Class<T> entityType, byte[] objectInBytes) {
		ByteArrayInputStream bis = new ByteArrayInputStream(objectInBytes);
		ObjectInput in = null;
		T object = null;
		try {
			in = new ObjectInputStream(bis);
			object = entityType.cast(in.readObject());
		} catch (ClassNotFoundException | IOException e) {
			LOG.log(Level.WARNING, e.getMessage(), e);
		} finally {
			try {
				bis.close();
			} catch (IOException ex) {
				// ignore close exception
			}
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException ex) {
				// ignore close exception
			}
		}
		return object;
	}

}