package milo.utils.impl;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import milo.utils.eventstore.EventHandler;
import milo.utils.eventstore.EventStore;
import milo.utils.eventstore.MessageBroker;
import milo.utils.eventstore.models.Event;

import javax.annotation.PreDestroy;
import javax.transaction.Transactional;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Transactional(Transactional.TxType.SUPPORTS)
public abstract class RabbitMqMessageBroker implements MessageBroker {

	private final String host;
	private static final Logger LOG = Logger.getLogger(RabbitMqMessageBroker.class.getName());
	private List<Connection> receiveConnections = new ArrayList<>();
	private List<Channel> receiveChannels = new ArrayList<>();

	private final String queuesToPurgeOnDeploy;

	public RabbitMqMessageBroker(String host, String queuesToPurgeOnDeploy) {
		this.host = host;
		this.queuesToPurgeOnDeploy = queuesToPurgeOnDeploy;
	}

	@Override
	public void publish(Event event) throws Exception {

		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(host);
		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();
		String exchange = event.getAggregateName();
		String routingKey = event.getClass().getSimpleName();
//		System.out.println(" ===== sending to exchange '" + exchange + "', with routingKey '" + routingKey + "'");
		channel.exchangeDeclare(exchange, "direct", true);
		channel.basicPublish(exchange, routingKey, null, serialize(event).getBytes());
		channel.close();
		connection.close();
	}

	@Override
	public abstract  <T extends Event> void subscribe(Class<T> eventType, EventHandler<T> eventHandler) throws Exception;

	public <T extends Event> void innerSubscribe(Class<T> eventType, EventHandler<T> eventHandler) throws Exception {
		T event = eventType.newInstance();

		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(host);
		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();

		String queueName = eventType.getName();
		String exchange = event.getAggregateName();
		String routingKey = event.getClass().getSimpleName();
//		channel.queueDeclarePassive(queueName);
//		String queue = channel.queueDeclare();
		String queue = channel.queueDeclare(queueName, true, false, false, null).getQueue();
		channel.exchangeDeclare(exchange, "direct", true);
		channel.queueBind(queue, exchange, routingKey);
		if (queuesToPurgeOnDeploy != null && queuesToPurgeOnDeploy.contains(queue)) {
			channel.queuePurge(queue);
		}
//		System.out.println(" ======= Waiting for messages in exchange '" + exchange + "', with routingKey '" + routingKey
//				+ "', queue (not important): " + queue);

//		QueueingConsumer consumer = new QueueingConsumer(channel);
		channel.basicConsume(queueName, false, (consumerTag, delivery) -> {
			try {
				String message = new String(delivery.getBody());
				T receivedEvent;
				try {
					EventStore.logEvent("subscribe", eventType.getSimpleName(), message);
					receivedEvent = this.deserialize(message, eventType);
				} catch (Exception e) {
					LOG.log(Level.WARNING, e.getMessage(), e);
					receivedEvent = null;
				}
				eventHandler.handle(receivedEvent);
				channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
			} catch (Exception e) {
				LOG.log(Level.WARNING, "caught subscription failure: " + eventType.getSimpleName(), e);
				if (channel.isOpen()) {
					try {
						channel.abort();
					} catch (IOException e1) {
						LOG.log(Level.WARNING, e1.getMessage(), e1);
					}
				}
			}
		}, consumerTag -> {});

		synchronized (this) {
			receiveConnections.add(connection);
			receiveChannels.add(channel);
		}
	}

	@PreDestroy
	private void closeConnections() {
		int connectionChannels = receiveChannels.size();
		receiveChannels.stream().filter(receiveChannel -> receiveChannel.isOpen()).forEach(receiveChannel -> {
			try {
				receiveChannel.abort();
			} catch (Throwable e) {
				LOG.log(Level.INFO, "MessageBroker.closeConnections caught connection.close(): " + e.getMessage());
			}
		});
		receiveConnections.stream().filter(receiveConnection -> receiveConnection.isOpen()).forEach(receiveConnection -> {
			try {
				receiveConnection.abort();
			} catch (Throwable e) {
				LOG.log(Level.INFO, "MessageBroker.closeConnections caught connection.close(): " + e.getMessage());
			}
		});
		System.out.println("MessageBroker.closeConnections ========================= " + connectionChannels);
	}

	protected abstract String serialize(Event event);

	protected abstract <T>T deserialize(String value, Class<T> type);

}
