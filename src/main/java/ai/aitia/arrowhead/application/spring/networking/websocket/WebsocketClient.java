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
import org.springframework.web.socket.WebSocketHandler;
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
import ai.aitia.arrowhead.application.common.networking.profile.Protocol;
import ai.aitia.arrowhead.application.common.networking.profile.model.QueryParams;
import ai.aitia.arrowhead.application.common.networking.profile.websocket.WebsocketKey;
import ai.aitia.arrowhead.application.common.verification.Ensure;

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
	private WebSocketHandler wsHandler;
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
	public void incomingMessage(Object message) {
		this.queue.add(message);
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void send(final Object payload) throws CommunicationException {
		send(null, payload);		
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void send(final QueryParams params, final Object payload) throws CommunicationException {
		try {
			sendMessage(params, payload);
		
		} catch (final DeveloperException ex) {
			throw ex;
		
		} catch (final InterruptedException | ExecutionException | TimeoutException | CommunicationException | IOException ex) {
			throw new CommunicationException(ex.getMessage(), ex);
		}		
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public <T> T receive(final Class<T> type) throws CommunicationException {
		//TODO
		return null;
	}

	//=================================================================================================
	// assistant methods
	
	//-------------------------------------------------------------------------------------------------
	private void sendMessage(final QueryParams params, final Object payload) throws InterruptedException, ExecutionException, TimeoutException, CommunicationException, IOException {
		Ensure.notNull(payload, "payload is null");
		
		if (this.wsClient == null) {
			this.wsClient = new StandardWebSocketClient();
			wsClient.getUserProperties().clear();
			wsClient.getUserProperties().put(TOMCAT_WS_SSL_CONTEXT, sslContext);
			final UriComponents uri = createURI(interfaceProfile.getAddress(), interfaceProfile.getPort(), params, interfaceProfile.get(String.class, WebsocketKey.PATH));
			final ListenableFuture<WebSocketSession> handshakeResult = this.wsClient.doHandshake(this.wsHandler, new WebSocketHttpHeaders(), uri.toUri());
			this.wsSession = handshakeResult.get(connectionTimeout, TimeUnit.MILLISECONDS);
			
		} else if(params != null) {
			throw new CommunicationException("Cannot send QueryParams after connection call");
		}
		
		wsSession.sendMessage(new BinaryMessage(this.objectMapper.writeValueAsBytes(payload)));		
	}
	
	//-------------------------------------------------------------------------------------------------
	private UriComponents createURI(final String host, final int port, final QueryParams queryParams, final String path) {
		final UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
		builder.scheme(Protocol.HTTP.name())
			   .host(host.trim())
			   .port(port);
		
		if (path != null && !path.isBlank()) {
			builder.path(path);
		}
		
		if (queryParams != null && queryParams.getParams().size() != 0) {
			if (queryParams.getParams().size() % 2 != 0) {
				//TODO throw new InvalidParameterException("queryParams variable arguments conatins a key without value");
			}
			
			final LinkedMultiValueMap<String, String> query = new LinkedMultiValueMap<>();
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
