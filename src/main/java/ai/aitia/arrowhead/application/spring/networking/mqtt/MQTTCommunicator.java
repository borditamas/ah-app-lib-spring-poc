package ai.aitia.arrowhead.application.spring.networking.mqtt;

import java.util.Properties;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.internal.security.SSLSocketFactoryFactory;

import ai.aitia.arrowhead.application.common.exception.InitializationException;
import ai.aitia.arrowhead.application.common.networking.CommunicationClient;
import ai.aitia.arrowhead.application.common.networking.CommunicationProperties;
import ai.aitia.arrowhead.application.common.networking.Communicator;
import ai.aitia.arrowhead.application.common.networking.CommunicatorType;
import ai.aitia.arrowhead.application.common.networking.decoder.PayloadDecoder;
import ai.aitia.arrowhead.application.common.networking.profile.InterfaceProfile;
import ai.aitia.arrowhead.application.common.verification.Ensure;

public class MQTTCommunicator implements Communicator {
	
	//=================================================================================================
	// members
	
	private CommunicationProperties props;
	private String clientName;
	private int connectionTimeout = 30;
	
	private final String brokerURI;
	private final String username;
	private final String password;
	private MqttClient brokerClient;
	private PayloadDecoder decoder;

	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public MQTTCommunicator(final String brokerAddress, final int brokerPort, final String username, final String password) {
		Ensure.notEmpty(brokerAddress, "brokerAddress is empty");
		Ensure.portRange(brokerPort);
		this.brokerURI = createAddress(brokerAddress, brokerPort);
		this.username = username;
		this.password = password;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public CommunicatorType type() {
		return CommunicatorType.MQTT;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void properties(final CommunicationProperties props) {
		this.props = props;
		
	}
	
	//-------------------------------------------------------------------------------------------------
	@Override
	public void decoder(PayloadDecoder decoder) {
		Ensure.notNull(decoder, "PayloadDecoder is null");
		this.decoder = decoder;			
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void initialize() {
		Ensure.notNull(props, "CommunicationProperties is null");
		try {
			this.brokerClient = new MqttClient(this.brokerURI, this.clientName);
			final MqttConnectOptions connOpts = new MqttConnectOptions();
			connOpts.setUserName(this.username);
			connOpts.setPassword(this.password.toCharArray());
			final Properties sslMQTTProperties = new Properties();
			sslMQTTProperties.put(SSLSocketFactoryFactory.KEYSTORE, this.props.getKeyStorePath().toAbsolutePath().toString());
			sslMQTTProperties.put(SSLSocketFactoryFactory.KEYSTOREPWD, this.props.getKeyStorePassword());
			sslMQTTProperties.put(SSLSocketFactoryFactory.KEYSTORETYPE, this.props.getKeyStoreType());
			sslMQTTProperties.put(SSLSocketFactoryFactory.TRUSTSTORE, this.props.getTrustStorePath().toAbsolutePath().toString());
			sslMQTTProperties.put(SSLSocketFactoryFactory.TRUSTSTOREPWD, this.props.getTrustStorePath());
			sslMQTTProperties.put(SSLSocketFactoryFactory.TRUSTSTORETYPE, this.props.getKeyStoreType()); //intentionally the same
			connOpts.setSSLProperties(sslMQTTProperties);
			connOpts.setCleanSession(true);
			connOpts.setConnectionTimeout(this.connectionTimeout);
			
			this.brokerClient.connect(connOpts);
			
		} catch (final Exception ex) {
			throw new InitializationException(ex.getMessage(), ex);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public boolean isInitialized() {
		return this.brokerClient != null && this.decoder != null;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public CommunicationClient client(final InterfaceProfile interfaceProfile) {
		return new MQTTClient(this.brokerClient, connectionTimeout, interfaceProfile, this.decoder);
	}

	//-------------------------------------------------------------------------------------------------
	public void setConnectionTimeout(final int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}
	
	//=================================================================================================
	// assistant methods
	
	//-------------------------------------------------------------------------------------------------
	private String createAddress(final String brokerAddress, final int brokerPort) {
		return "ssl://" + brokerAddress + ":" + brokerPort;
	}
}
