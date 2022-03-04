package ai.aitia.arrowhead.application.spring.networking.websocket;

import java.io.IOException;
import java.nio.ByteBuffer;
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
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.aitia.arrowhead.application.common.exception.CommunicationException;
import ai.aitia.arrowhead.application.common.exception.DeveloperException;
import ai.aitia.arrowhead.application.common.networking.CommunicationClient;
import ai.aitia.arrowhead.application.common.networking.CommunicationProperties;
import ai.aitia.arrowhead.application.common.networking.decoder.PayloadDecoder;
import ai.aitia.arrowhead.application.common.networking.decoder.PayloadResolver;
import ai.aitia.arrowhead.application.common.networking.profile.InterfaceProfile;
import ai.aitia.arrowhead.application.common.networking.profile.MessageProperties;
import ai.aitia.arrowhead.application.common.networking.profile.Protocol;
import ai.aitia.arrowhead.application.common.networking.profile.http.HttpsKey;
import ai.aitia.arrowhead.application.common.networking.profile.model.PathVariables;
import ai.aitia.arrowhead.application.common.networking.profile.model.QueryParams;
import ai.aitia.arrowhead.application.common.networking.profile.websocket.WebsocketKey;
import ai.aitia.arrowhead.application.common.networking.profile.websocket.WebsocketMsgKey;
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
	private final PayloadDecoder decoder;
	
	private StandardWebSocketClient wsClient;
	private WebSocketSession wsSession;
	
	private final BlockingQueue<WebSocketMessage<?>> queue = new LinkedBlockingQueue<>();

	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public WebsocketClient(final String clientName, final CommunicationProperties props, final SSLContext sslContext, final int connectionTimeout, final InterfaceProfile interfaceProfile,
						   final PayloadDecoder payloadDecoder) {
		this.clientName = clientName;
		this.props = props;
		this.sslContext = sslContext;
		this.connectionTimeout  = connectionTimeout;
		this.decoder = payloadDecoder;
		
		Ensure.notNull(this.props, "CommunicationProperties is null");
		Ensure.notNull(this.decoder, "PayloadDecoder is null");
		Ensure.notNull(interfaceProfile, "interfaceProfile is null");
		Ensure.isTrue(interfaceProfile.getProtocol() == Protocol.WEBSOCKET, "Invalid protocol for WebsocketClient: " + interfaceProfile.getProtocol().name());
		Ensure.notEmpty(interfaceProfile.get(String.class, HttpsKey.ADDRESS), "address is empty");
		Ensure.portRange(interfaceProfile.get(Integer.class, HttpsKey.PORT));
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
	public void receive(final PayloadResolver payloadResolver) throws CommunicationException {
		Ensure.notNull(payloadResolver, "PayloadResolver cannot be null");
		
		try {
			final WebSocketMessage<?> msg = this.queue.take();
			if (msg.getPayload() == null) {
				payloadResolver.add(msg);
				return;
			}
			
			if (msg.getPayload() instanceof ByteBuffer) {
				final ByteBuffer buffer = (ByteBuffer)msg.getPayload();
				payloadResolver.add(this.decoder, buffer.array(), msg);
				
			} else if (msg.getPayload() instanceof String) {
				final String payloadStr  = (String)msg.getPayload();
				payloadResolver.add(this.decoder, payloadStr, msg);
				
			} else {
				throw new CommunicationException("Unkown websocket message payload type");
			}
			
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
			final UriComponents uri = createURI(this.interfaceProfile.get(String.class, HttpsKey.ADDRESS), this.interfaceProfile.get(Integer.class, HttpsKey.PORT),
												this.interfaceProfile.get(String.class, WebsocketKey.PATH),	props_.get(PathVariables.class, WebsocketMsgKey.PATH_VARIABLES),
												props_.get(QueryParams.class, WebsocketMsgKey.QUERY_PARAMETERS));
			
			final ListenableFuture<WebSocketSession> handshakeResult = this.wsClient.doHandshake(new WebsocketHandler(this.queue,
																													  this.interfaceProfile.getOrDefault(Boolean.class, WebsocketKey.PARTIAL_MSG_SUPPORT, false)),
																													  new WebSocketHttpHeaders(),
																													  uri.toUri());
			this.wsSession = handshakeResult.get(connectionTimeout, TimeUnit.MILLISECONDS);
		
		} else if(props_.get(PathVariables.class, WebsocketMsgKey.PATH_VARIABLES) != null) {
			throw new CommunicationException("Cannot send PathVariables after connection call");
			
		} else if(props_.get(QueryParams.class, WebsocketMsgKey.QUERY_PARAMETERS) != null) {
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
			Ensure.isTrue(queryParams.getParams().size() % 2 == 0, "queryParams variable arguments conatins a key without value");			
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
