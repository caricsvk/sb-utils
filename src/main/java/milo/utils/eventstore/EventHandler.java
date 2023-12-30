package milo.utils.eventstore;

import milo.utils.eventstore.models.Event;

import jakarta.persistence.NoResultException;

/**
 * Created by caric on 1/21/15.
 */
public interface EventHandler<T extends Event> {

	public void handle(T event) throws NoResultException;
}
