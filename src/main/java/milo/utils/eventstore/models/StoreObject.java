package milo.utils.eventstore.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class StoreObject<T extends Aggregate<T>> implements Serializable {

	private static final long serialVersionUID = -7697145954457966992L;

	private T currentSnapshot;
	private List<Event> currentEvents;

	protected int getSnapshotOccurrence() {
		return 100;
	}

	public StoreObject(T aggregate, List<Event> currentEvents) {
		this.currentSnapshot = aggregate;
		this.currentEvents = currentEvents;
	}

	public T getCurrentSnapshot() {
		return currentSnapshot;
	}

	public List<Event> getCurrentEvents() {
		if (currentEvents == null) {
			currentEvents = new ArrayList<>();
		}
		return currentEvents;
	}

	public synchronized void addEvents(List<Event> newEvents) {
		currentEvents.addAll(newEvents);
		if (currentEvents.size() > getSnapshotOccurrence() * 2) {
			List<Event> oldList = currentEvents.subList(0, currentEvents.size() - getSnapshotOccurrence());
			currentSnapshot = currentSnapshot.applyEvents(oldList);
			currentEvents.removeAll(oldList);
		}
	}

}