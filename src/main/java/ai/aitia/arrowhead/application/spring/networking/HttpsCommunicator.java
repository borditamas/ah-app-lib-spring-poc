package ai.aitia.arrowhead.application.spring.networking;

import ai.aitia.arrowhead.application.common.exception.CommunicationException;
import ai.aitia.arrowhead.application.common.exception.DeveloperException;
import ai.aitia.arrowhead.application.common.networking.Communicator;
import ai.aitia.arrowhead.application.common.networking.CommunicatorType;
import ai.aitia.arrowhead.application.common.networking.SSLProperties;
import ai.aitia.arrowhead.application.common.networking.profile.InterfaceProfile;
import ai.aitia.arrowhead.application.common.networking.profile.Protocol;
import ai.aitia.arrowhead.application.common.verification.Ensure;

public class HttpsCommunicator implements Communicator {
	
	private SSLProperties sslProps;
	private boolean initialized = false;

	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	@Override
	public CommunicatorType getType() {
		return CommunicatorType.HTTPS;
	}
	
	//-------------------------------------------------------------------------------------------------
	@Override
	public void loadSSLProperties(final SSLProperties sslProps) {
		Ensure.notNull(sslProps, "SSLProperties is null");
		sslProps.verify();
		// TODO Auto-generated method stub		
	}
	
	//-------------------------------------------------------------------------------------------------
	@Override
	public void initialize() {
		Ensure.notNull(this.sslProps, "SSLProperties is null");
		// TODO Auto-generated method stub
		this.initialized = true;
		
	}
	
	//-------------------------------------------------------------------------------------------------
	@Override
	public boolean isInitialized() {
		return this.initialized;
	}
	
	//-------------------------------------------------------------------------------------------------
	@Override
	public <T> T send(final InterfaceProfile interfaceProfile, final Class<T> responseType) throws CommunicationException {
		return send(interfaceProfile, responseType, null);
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public <T,P> T send(final InterfaceProfile interfaceProfile, final Class<T> responseType, final P payload) throws CommunicationException {
		try {
			return sendRequest(interfaceProfile, responseType, payload);			
				
		} catch (final DeveloperException ex) {
			throw ex;
				
		} catch (final Exception ex) {
			// log
			throw new CommunicationException(ex.getMessage());
		}
	}
	
	//=================================================================================================
	// assistant methods
	
	//-------------------------------------------------------------------------------------------------
	private <T,P> T sendRequest(final InterfaceProfile interfaceProfile, final Class<T> responseType, final P payload) {
		Ensure.notNull(interfaceProfile, "interfaceProfile is null");
		Ensure.isTrue(interfaceProfile.getProtocol() == Protocol.HTTP, "Invalid protocol for HttpsCommunicator: " + interfaceProfile.getProtocol().name());
		Ensure.notNull(interfaceProfile.getAddress(), "address is null");
		Ensure.portRange(interfaceProfile.getPort());
		Ensure.notNull(responseType, "responseType is null");
		
		//TODO
		
		return null;
	}
}
