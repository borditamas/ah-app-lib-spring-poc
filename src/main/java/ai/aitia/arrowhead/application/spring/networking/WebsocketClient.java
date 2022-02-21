package ai.aitia.arrowhead.application.spring.networking;

import ai.aitia.arrowhead.application.common.exception.CommunicationException;
import ai.aitia.arrowhead.application.common.networking.CommunicationClient;
import ai.aitia.arrowhead.application.common.networking.profile.InterfaceProfile;

public class WebsocketClient implements CommunicationClient {

	//=================================================================================================
	// methods
	
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
	public <T,P> T send(InterfaceProfile interfaceProfile, Class<T> responseType, P payload)
			throws CommunicationException {
		// TODO Auto-generated method stub
		return null;
	}

}
