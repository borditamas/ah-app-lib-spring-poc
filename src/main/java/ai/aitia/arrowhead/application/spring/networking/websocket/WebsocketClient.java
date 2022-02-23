package ai.aitia.arrowhead.application.spring.networking.websocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.SSLContext;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.aitia.arrowhead.application.common.exception.CommunicationException;
import ai.aitia.arrowhead.application.common.exception.DeveloperException;
import ai.aitia.arrowhead.application.common.networking.CommunicationClient;
import ai.aitia.arrowhead.application.common.networking.CommunicationProperties;
import ai.aitia.arrowhead.application.common.networking.profile.InterfaceProfile;
import ai.aitia.arrowhead.application.common.networking.profile.MessageProperties;
import ai.aitia.arrowhead.application.common.networking.profile.Protocol;
import ai.aitia.arrowhead.application.common.networking.profile.model.PathVariables;
import ai.aitia.arrowhead.application.common.networking.profile.model.QueryParams;
import ai.aitia.arrowhead.application.common.networking.profile.websocket.WebsocketKey;
import ai.aitia.arrowhead.application.common.verification.Ensure;
import ai.aitia.arrowhead.application.spring.networking.websocket.handler.WebsocketHandler;

public class WebsocketClient implements CommunicationClient {
	
	//=================================================================================================
	// members
	
	private static final String TOMCAT_WS_SSL_CONTEXT = "org.apache.tomcat.websocket.SSL_CONTEXT";
	private final ObjectMapper objectMapper = new ObjectMapper();
	
	private final String clientName;
	private final CommunicationProperties props;
	private final SSLContext sslContext;
	private final int connectionTimeout;
	private final InterfaceProfile interfaceProfile;
	
	private StandardWebSocketClient wsClient;
	private WebSocketSession wsSession;
	
	private final BlockingQueue<Object> queue = new LinkedBlockingQueue<>();

	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public WebsocketClient(final String clientName, final CommunicationProperties props, final SSLContext sslContext, final int connectionTimeout, final InterfaceProfile interfaceProfile) {
		this.clientName = clientName;
		this.props = props;
		this.sslContext = sslContext;
		this.connectionTimeout  = connectionTimeout;
		
		Ensure.notNull(this.props, "CommunicationProperties is null");
		Ensure.notNull(interfaceProfile, "interfaceProfile is null");
		Ensure.isTrue(interfaceProfile.getProtocol() == Protocol.WEBSOCKET, "Invalid protocol for WebsocketClient: " + interfaceProfile.getProtocol().name());
		Ensure.notEmpty(interfaceProfile.getAddress(), "address is empty");
		Ensure.portRange(interfaceProfile.getPort());
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
		
		} catch (final InterruptedException | ExecutionException | TimeoutException | CommunicationException | IOException ex) {
			throw new CommunicationException(ex.getMessage(), ex);
		}		
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Override
	public <T> T receive(final Class<T> type) throws CommunicationException {
		try {
			final Object received = this.queue.take();
			if (received instanceof Throwable) {
				throw (Throwable)received;
			}
			Ensure.isTrue(type.isAssignableFrom(received.getClass()), "Message cannot be casted to" + type.getSimpleName());
			return (T)received;
			
		} catch (final DeveloperException ex) {
			throw ex;
			
		} catch (final Throwable ex) {
			throw new CommunicationException(ex.getMessage(), ex);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	@Override
	public void terminate() throws CommunicationException {
		try {
			this.wsSession.close();
		} catch (final IOException ex) {
			throw new CommunicationException(ex.getMessage(), ex);
		}
	}

	//=================================================================================================
	// assistant methods
	
	//-------------------------------------------------------------------------------------------------
	private void sendMessage(final MessageProperties props, final Object payload) throws InterruptedException, ExecutionException, TimeoutException, CommunicationException, IOException {
		Ensure.notNull(payload, "payload is null");
		final MessageProperties props_ = props != null ? props : new MessageProperties();
		
		if (this.wsClient == null) {
			this.wsClient = new StandardWebSocketClient();
			this.wsClient.getUserProperties().clear();
			this.wsClient.getUserProperties().put(TOMCAT_WS_SSL_CONTEXT, sslContext);
			final UriComponents uri = createURI(this.interfaceProfile.getAddress(), interfaceProfile.getPort(), this.interfaceProfile.get(String.class, WebsocketKey.PATH),
												props_.get(PathVariables.class, WebsocketKey.PATH_VARIABLES), props_.get(QueryParams.class, WebsocketKey.QUERY_PARAMETERS));
			final ListenableFuture<WebSocketSession> handshakeResult = this.wsClient.doHandshake(new WebsocketHandler(this.queue,
																													  this.interfaceProfile.getOrDefault(Boolean.class, WebsocketKey.PARTIAL_MSG_SUPPORT, false)),
																													  new WebSocketHttpHeaders(),
																													  uri.toUri());
			this.wsSession = handshakeResult.get(connectionTimeout, TimeUnit.MILLISECONDS);
		
		} else if(props_.get(PathVariables.class, WebsocketKey.PATH_VARIABLES) != null) {
			throw new CommunicationException("Cannot send QueryParams after connection call");
			
		} else if(props_.get(QueryParams.class, WebsocketKey.QUERY_PARAMETERS) != null) {
			throw new CommunicationException("Cannot send QueryParams after connection call");
		}
		
		this.wsSession.sendMessage(new BinaryMessage(this.objectMapper.writeValueAsBytes(payload)));		
	}
	
	//-------------------------------------------------------------------------------------------------
	private UriComponents createURI(final String host, final int port, final String path, final PathVariables pathVars, final QueryParams queryParams) {
		final UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
		builder.scheme(Protocol.HTTP.name()) //First time it is HTTP than the server will upgrade to WEBSOCKET
			   .host(host.trim())
			   .port(port);
		
		if (path != null && !path.isBlank()) {
			builder.path(path);
			builder.pathSegment(pathVars.getVariables().toArray(new String[pathVars.getVariables().size()]));
		}
		
		if (queryParams != null && queryParams.getParams().size() != 0) {
			if (queryParams.getParams().size() % 2 != 0) {
				//TODO throw new InvalidParameterException("queryParams variable arguments conatins a key without value");
			}
			
			final LinkedMultiValueMap<String,String> query = new LinkedMultiValueMap<>();
			int count = 1;
			String key = "";
			for (final String vararg : queryParams.getParams()) {
				if (count % 2 != 0) {
					query.putIfAbsent(vararg, new ArrayList<>());
					key = vararg;
				} else {
					query.get(key).add(vararg);
				}
				count++;
			}
			builder.queryParams(query);			
		}

		return builder.build();
	}
}
