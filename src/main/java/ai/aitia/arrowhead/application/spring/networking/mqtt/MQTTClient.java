package ai.aitia.arrowhead.application.spring.networking.mqtt;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.aitia.arrowhead.application.common.exception.CommunicationException;
import ai.aitia.arrowhead.application.common.exception.DeveloperException;
import ai.aitia.arrowhead.application.common.networking.CommunicationClient;
import ai.aitia.arrowhead.application.common.networking.decoder.PayloadDecoder;
import ai.aitia.arrowhead.application.common.networking.decoder.PayloadResolver;
import ai.aitia.arrowhead.application.common.networking.profile.InterfaceProfile;
import ai.aitia.arrowhead.application.common.networking.profile.MessageProperties;
import ai.aitia.arrowhead.application.common.networking.profile.model.PathVariables;
import ai.aitia.arrowhead.application.common.networking.profile.mqtt.MqttKey;
import ai.aitia.arrowhead.application.common.networking.profile.mqtt.MqttMsgKey;
import ai.aitia.arrowhead.application.common.verification.Ensure;

public class MQTTClient implements CommunicationClient {
	
	//=================================================================================================
	// members

	private static final int QOS_AT_MOST_ONCE = 0; // message loss is acceptable and it does not require any kind of acknowledgment or persistence
	//private static final int QOS_AT_LEAST_ONCE = 1; // message loss is not acceptable and subscriber can handle duplicates
	private static final int QOS_EXACTLY_ONCE = 2; // message loss is not acceptable and subscriber cannot handle duplicates
	private final ObjectMapper objectMapper = new ObjectMapper();
	
	private String publishTopic = null;
	private String subscribeTopic = null;
	private boolean receiveTimeout = false;
	
	private final MqttClient brokerClient;
	private final int connectionTimeout;
	private final InterfaceProfile interfaceProfile;
	private final PayloadDecoder decoder;
	
	private final BlockingQueue<MqttMessage> queue = new LinkedBlockingQueue<>();
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public MQTTClient(final MqttClient brokerClient, final int connectionTimeout, final InterfaceProfile interfaceProfile, final PayloadDecoder payloadDecoder) {
		Ensure.notNull(brokerClient, "brokerClient is null");
		Ensure.notNull(interfaceProfile, "interfaceProfile is null");
		Ensure.notNull(payloadDecoder, "PayloadDecoder is null");
		
		this.brokerClient = brokerClient;
		this.connectionTimeout = connectionTimeout;
		this.interfaceProfile = interfaceProfile;
		this.decoder = payloadDecoder;
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
	public void receive(final PayloadResolver payloadResolver) throws CommunicationException {
		if (this.subscribeTopic == null) {
			throw new CommunicationException("Not subscribed to any topic");
		}
		Ensure.notNull(payloadResolver, "PayloadResolver cannot be null");
		
		try {
			final MqttMessage msg;
			if (receiveTimeout) {
				msg = this.queue.poll(this.connectionTimeout, TimeUnit.SECONDS);
			} else {
				msg = this.queue.take();
			}
			
			if (msg.getPayload() == null || msg.getPayload().length == 0) {
				payloadResolver.add(msg);
				
			} else {
				payloadResolver.add(this.decoder, msg.getPayload(), msg);
			}
			
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
	private void sendMessage(final MessageProperties props, final Object payload) throws JsonProcessingException, MqttPersistenceException, MqttException, CommunicationException {
		Ensure.notNull(payload, "payload is null");
		subscribeIfNotYet(props);
		
		final MessageProperties props_ = props != null ? props : new MessageProperties();
		if (this.publishTopic == null && this.interfaceProfile.contains(MqttKey.TOPIC_PUBLISH)) {
			this.publishTopic = createTopicUri(this.interfaceProfile.get(String.class, MqttKey.TOPIC_PUBLISH),
					 						   props_.get(PathVariables.class, MqttMsgKey.PATH_VARIABLES_PUBLISH));
		}
		
		if (this.publishTopic != null && payload != null) {
			this.brokerClient.publish(this.publishTopic,
									  this.objectMapper.writeValueAsBytes(payload),
									  props_.getOrDefault(Integer.class, MqttMsgKey.QOS, QOS_AT_MOST_ONCE),
									  props_.getOrDefault(Boolean.class, MqttMsgKey.RETAINED, false));			
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	private void subscribeIfNotYet(final MessageProperties props) throws CommunicationException {
		final MessageProperties props_ = props != null ? props : new MessageProperties();
		if (this.subscribeTopic == null && this.interfaceProfile.contains(MqttKey.TOPIC_SUBSCRIBE)) {
			this.subscribeTopic = createTopicUri(this.interfaceProfile.get(String.class, MqttKey.TOPIC_SUBSCRIBE),
												 props_.get(PathVariables.class, MqttMsgKey.PATH_VARIABLES_SUBSCRIBE));
			
			try {
				this.brokerClient.subscribe(this.subscribeTopic, QOS_EXACTLY_ONCE, (topic, msg) -> { // TODO QoS to come from MsgProps
					if (topic.equals(this.subscribeTopic)) {
						this.queue.add(msg);
					}
				});
				
			} catch (final MqttException ex) {
				throw new CommunicationException(ex.getMessage(), ex);
			}
		}
		
		this.receiveTimeout = props_.getOrDefault(Boolean.class, MqttMsgKey.RECEIVE_TIMEOUT, false);
	}
	
	//-------------------------------------------------------------------------------------------------
	private String createTopicUri(final String topic, final PathVariables pathVars) {
		final StringBuilder sb = new StringBuilder(topic == null || topic.isBlank() ? "" : topic);
		for (final String var : pathVars.getVariables()) {
			sb.append("/" + var);
		}
		return sb.toString();
	}
}
