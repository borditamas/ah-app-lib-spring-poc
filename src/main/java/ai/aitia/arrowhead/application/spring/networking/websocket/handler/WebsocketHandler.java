package ai.aitia.arrowhead.application.spring.networking.websocket.handler;

import java.util.concurrent.BlockingQueue;

import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

public class WebsocketHandler implements WebSocketHandler {
	
	//=================================================================================================
	// members
	
	private final BlockingQueue<WebSocketMessage<?>> queue;
	private final boolean partialMsgSupport;

	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public WebsocketHandler(final BlockingQueue<WebSocketMessage<?>> queue, final boolean partialMsgSupport) {
		this.queue = queue;
		this.partialMsgSupport = partialMsgSupport;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void afterConnectionEstablished(final WebSocketSession session) throws Exception {
		// do nothing		
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void handleMessage(final WebSocketSession session, final WebSocketMessage<?> message) throws Exception {
		this.queue.add(message);
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void handleTransportError(final WebSocketSession session, final Throwable exception) throws Exception {
		//this.queue.add(exception);		
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void afterConnectionClosed(final WebSocketSession session, final CloseStatus closeStatus) throws Exception {
		// do nothing
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public boolean supportsPartialMessages() {
		return this.partialMsgSupport;
	}
}
