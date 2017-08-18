package milo.utils.eventstore;

import milo.utils.eventstore.models.Aggregate;
import milo.utils.eventstore.models.Event;

import java.util.List;

/**
 * Created by caric on 1/18/15.
 */
public interface CommandProcessingAggregate<T> extends Aggregate<T> {

	public List<Event> processCommands();
}
