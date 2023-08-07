package milo.utils.impl;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import milo.utils.eventstore.EventStore;
import milo.utils.eventstore.MessageBroker;
import milo.utils.eventstore.exceptions.AggregateAccessException;
import milo.utils.eventstore.models.Aggregate;
import milo.utils.eventstore.models.StoreObject;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.logging.Logger;

@Transactional(Transactional.TxType.SUPPORTS)
public class BerkeleyDBEventStore extends EventStore {

	private static final Logger LOG = Logger.getLogger(BerkeleyDBEventStore.class.getName());
	private Database database;

	@Inject
	public BerkeleyDBEventStore(
			BerkeleyDBFactory berkeleyDBFactory,
			MessageBroker messageBroker
	) {
		super(messageBroker);
		this.database = berkeleyDBFactory.getDatabase();
	}

	@Override
	protected <T extends Aggregate<T>> void put(StoreObject<T> storeObject) {
		String key = storeObject.getCurrentSnapshot().getClass().getName()
				+ storeObject.getCurrentSnapshot().getAggregateId();
		DatabaseEntry databaseEntryKey = new DatabaseEntry(key.getBytes());
		DatabaseEntry databaseEntryValue = new DatabaseEntry(marshallObject(storeObject));
		database.put(null, databaseEntryKey, databaseEntryValue);
	}

	@Override
	protected <T extends Aggregate<T>> StoreObject getStoredObject(Class<T> entityType, String aggregateId) {
		String key = entityType.getName() + aggregateId;
		DatabaseEntry databaseEntryKey = new DatabaseEntry(key.getBytes());
		DatabaseEntry databaseEntryValue = new DatabaseEntry();
		if (database.get(null, databaseEntryKey, databaseEntryValue, LockMode.DEFAULT).equals(OperationStatus.SUCCESS)) {
			return unmarshallObject(StoreObject.class, databaseEntryValue.getData());
		} else {
			throw new AggregateAccessException("aggregate " + aggregateId + " [" + entityType.getName()
					+ "] was not found").asNotFound();
		}
	}

	@Override
	public <T extends Aggregate<T>> void delete(Class<T> entityType, String aggregateId) {
		String key = entityType.getName() + aggregateId;
		database.delete(null, new DatabaseEntry(key.getBytes()));
	}

}
