package ai.aitia.arrowhead.application.spring.core.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ai.aitia.arrowhead.application.common.networking.profile.CommunicatorProfile;
import ai.aitia.arrowhead.application.common.service.MonitoringService;
import ai.aitia.arrowhead.application.core.mandatory.systemregistry.SystemRegistryClient;
import ai.aitia.arrowhead.application.spring.core.CoreClientBean;

@Component
public class SystemRegistryClientBean implements CoreClientBean {

	//=================================================================================================
	// members
	
	private SystemRegistryClient client;
	
	@Autowired
	private ServiceRegistryClientBean serviceRegistry;
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public void initialize(final CommunicatorProfile communicatorProfile) {
		this.client = new SystemRegistryClient(communicatorProfile, serviceRegistry.getClient());
		client.initialize();
	}
	
	//-------------------------------------------------------------------------------------------------
	@Override
	public boolean isInitialized() {
		return this.client.isInitialized();
	}
	
	//=================================================================================================
	// services
	
	//-------------------------------------------------------------------------------------------------
	public MonitoringService monitoringService() {
		return this.client.monitoringService();
	}
}
