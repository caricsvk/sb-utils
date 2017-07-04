package milo.utils.eventstore.models;

import java.io.Serializable;
import java.util.List;

/**
 * Created by caric on 1/18/15.
 */
public interface Aggregate<T> extends Serializable {

	public String getAggregateId();
	public Integer getAggregateVersion();
	public void setAggregateId(String id, Integer version);
	public abstract T applyEvents(List<Event> events);

}