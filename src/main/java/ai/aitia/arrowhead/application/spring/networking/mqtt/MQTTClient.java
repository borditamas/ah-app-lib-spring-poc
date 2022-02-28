package ai.aitia.arrowhead.application.spring.networking.mqtt;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.aitia.arrowhead.application.common.exception.CommunicationException;
import ai.aitia.arrowhead.application.common.exception.DeveloperException;
import ai.aitia.arrowhead.application.common.networking.CommunicationClient;
import ai.aitia.arrowhead.application.common.networking.profile.InterfaceProfile;
import ai.aitia.arrowhead.application.common.networking.profile.MessageProperties;
import ai.aitia.arrowhead.application.common.networking.profile.mqtt.MqttKey;
import ai.aitia.arrowhead.application.common.verification.Ensure;

public class MQTTClient implements CommunicationClient {

	private static final int QOS_AT_MOST_ONCE = 0; // message loss is acceptable and it does not require any kind of acknowledgment or persistence
	//private static final int QOS_AT_LEAST_ONCE = 1; // message loss is not acceptable and subscriber can handle duplicates
	private static final int QOS_EXACTLY_ONCE = 2; // message loss is not acceptable and subscriber cannot handle duplicates
	private final ObjectMapper objectMapper = new ObjectMapper();
	private String subscribeTopic = null;
	private String publishTopic = null;
	
	private final MqttClient brokerClient;
	private final InterfaceProfile interfaceProfile;
	
	private final BlockingQueue<MqttMessage> queue = new LinkedBlockingQueue<>();
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public MQTTClient(final MqttClient brokerClient, final InterfaceProfile interfaceProfile) {
		Ensure.notNull(brokerClient, "brokerClient is null");
		Ensure.notNull(interfaceProfile, "interfaceProfile is null");
		this.brokerClient = brokerClient;
		this.interfaceProfile = interfaceProfile;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void send(final Object payload) throws CommunicationException {
		send(null, payload);		
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void send(final MessageProperties props, final Object payload) throws CommunicationException {
		try {
			sendMessage(props, payload);
			
		} catch (final DeveloperException ex) {
			throw ex;
			
		} catch (final JsonProcessingException | MqttException ex) {
			throw new CommunicationException(ex.getMessage(), ex);
		}		
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public <T> T receive(final Class<T> type) throws CommunicationException {
		subscribeIfNotYet();
		
		try {
			final MqttMessage msg = this.queue.take();
			return objectMapper.readValue(msg.getPayload(), type);
			
		} catch (final DeveloperException ex) {
			throw ex;
			
		} catch (final Exception ex) {
			throw new CommunicationException(ex.getMessage(), ex);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void terminate() throws CommunicationException {
		if (this.subscribeTopic != null) {
			try {
				this.brokerClient.unsubscribe(this.subscribeTopic);
				this.subscribeTopic = null;
			} catch (final MqttException ex) {
				throw new CommunicationException(ex.getMessage(), ex);
			}
		}
	}
	
	//=================================================================================================
	// assistant methods
	
	//-------------------------------------------------------------------------------------------------
	private void sendMessage(final MessageProperties props, final Object payload) throws JsonProcessingException, MqttPersistenceException, MqttException {
		Ensure.notNull(payload, "payload is null");
		final MessageProperties props_ = props != null ? props : new MessageProperties();
		if (this.publishTopic == null) {
			this.publishTopic = this.interfaceProfile.getOrDefault(String.class, MqttKey.TOPIC_PUBLISH, "/");
		}
		
		this.brokerClient.publish(this.publishTopic,
								  this.objectMapper.writeValueAsBytes(payload),
								  props_.getOrDefault(Integer.class, MqttKey.QOS, QOS_AT_MOST_ONCE),
								  props.getOrDefault(Boolean.class, MqttKey.RETAINED, false));
	}
	
	//-------------------------------------------------------------------------------------------------
	private void subscribeIfNotYet() throws CommunicationException {
		if (this.subscribeTopic == null) {
			this.subscribeTopic = this.interfaceProfile.getOrDefault(String.class, MqttKey.TOPIC_SUBSCRIBE, "/");
			try {
				this.brokerClient.subscribe(this.subscribeTopic, QOS_EXACTLY_ONCE, (topic, msg) -> {
					if (topic.equals(this.subscribeTopic)) {
						this.queue.add(msg);
					}
				});
				
			} catch (final MqttException ex) {
				throw new CommunicationException(ex.getMessage(), ex);
			}
		}
	}
}
