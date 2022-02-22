package ai.aitia.arrowhead.application.spring.core.client;

import org.springframework.stereotype.Component;

import ai.aitia.arrowhead.application.common.networking.profile.CommunicatorProfile;
import ai.aitia.arrowhead.application.common.networking.profile.InterfaceProfile;
import ai.aitia.arrowhead.application.common.service.MonitoringService;
import ai.aitia.arrowhead.application.core.mandatory.serviceregistry.ServiceRegistryClient;
import ai.aitia.arrowhead.application.core.mandatory.serviceregistry.service.ServiceDiscoveryService;
import ai.aitia.arrowhead.application.spring.core.CoreClientBean;

@Component
public class ServiceRegistryClientBean implements CoreClientBean {

	//=================================================================================================
	// members
	
	private ServiceRegistryClient client;
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public void initialize(final CommunicatorProfile communicatorProfile, final InterfaceProfile queryInterfaceProfile) {
		this.client = new ServiceRegistryClient(communicatorProfile, queryInterfaceProfile);
		this.client.initialize();
	}
	
	//-------------------------------------------------------------------------------------------------
	@Override
	public boolean isInitialized() {
		return this.client.isInitialized();
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
