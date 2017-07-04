package milo.utils.eventstore.models;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Created by caric on 1/18/15.
 */
public interface Event extends Serializable {

	String getAggregateName();
	String getEventName();
	LocalDateTime getCreated();

}
