package ai.aitia.arrowhead.application.spring.core.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ai.aitia.arrowhead.application.common.networking.HttpsCommunicator;
import ai.aitia.arrowhead.application.common.service.MonitoringService;
import ai.aitia.arrowhead.application.core.mandatory.systemregistry.SystemRegistryClient;

@Component
public class SystemRegistryHTTPClient {

	//=================================================================================================
	// members
	
	private SystemRegistryClient client;
	
	@Autowired
	private ServiceRegistryHTTPClient serviceRegistry;
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public void init() {
		this.client = new SystemRegistryClient(new HttpsCommunicator(), serviceRegistry.getClient());
	}
	
	//=================================================================================================
	// services
	
	//-------------------------------------------------------------------------------------------------
	public MonitoringService monitoringService() {
		return this.client.monitoringService();
	}
}
