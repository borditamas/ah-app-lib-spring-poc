package ai.aitia.arrowhead.application.spring.core.http;

import org.springframework.stereotype.Component;

import ai.aitia.arrowhead.application.common.networking.CommunicationClient;
import ai.aitia.arrowhead.application.common.networking.Communicator;
import ai.aitia.arrowhead.application.common.networking.profile.InterfaceProfile;
import ai.aitia.arrowhead.application.common.service.MonitoringService;
import ai.aitia.arrowhead.application.core.mandatory.serviceregistry.ServiceRegistryClient;
import ai.aitia.arrowhead.application.core.mandatory.serviceregistry.service.ServiceDiscoveryService;
import ai.aitia.arrowhead.application.spring.core.CoreClientBean;
import ai.aitia.arrowhead.application.spring.networking.http.HttpsCommunicator;

@Component
public class ServiceRegistryHTTPClient implements CoreClientBean {

	//=================================================================================================
	// members
	
	private ServiceRegistryClient client;
	private boolean initialized = false;
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public void initialize(final InterfaceProfile queryInterfaceProfile) {
		this.client = new ServiceRegistryClient(new HttpsCommunicator(), queryInterfaceProfile);
		this.client.initialize();
		this.initialized = true;
	}
	
	//-------------------------------------------------------------------------------------------------
	@Override
	public boolean isInitialized() {
		return this.initialized;
	}
	
	//-------------------------------------------------------------------------------------------------
	public ServiceRegistryClient getClient() {
		return this.client;
	}
	
	//=================================================================================================
	// services
	
	//-------------------------------------------------------------------------------------------------
	public MonitoringService monitoringService() {
		return this.client.monitoringService();
	}
	
	//-------------------------------------------------------------------------------------------------
	public ServiceDiscoveryService serviceDiscoveryService() {
		return this.client.serviceDiscoveryService();
	}
}
