package ai.aitia.arrowhead.application.spring.networking;

import javax.net.ssl.SSLContext;

import ai.aitia.arrowhead.application.common.networking.CommunicationClient;
import ai.aitia.arrowhead.application.common.networking.CommunicationProperties;
import ai.aitia.arrowhead.application.common.networking.Communicator;
import ai.aitia.arrowhead.application.common.networking.CommunicatorType;

public class WebsocketCommunicator implements Communicator<CommunicationClient> {
	
	//=================================================================================================
	// members
	
	private CommunicationProperties props;
	private String clientName;
	private SSLContext sslContext;

	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	@Override
	public CommunicatorType type() {
		return CommunicatorType.WEBSOCKET;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void properties(final CommunicationProperties props) {
		this.props = props;	
		
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public boolean isInitialized() {
		// TODO Auto-generated method stub
		return false;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public CommunicationClient client() {
		return new WebsocketClient();
	}

}
