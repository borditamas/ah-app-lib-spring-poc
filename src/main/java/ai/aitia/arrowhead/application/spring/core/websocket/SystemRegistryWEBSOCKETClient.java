package ai.aitia.arrowhead.application.spring.core.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ai.aitia.arrowhead.application.common.networking.WebsocketCommunicator;
import ai.aitia.arrowhead.application.common.service.MonitoringService;
import ai.aitia.arrowhead.application.core.mandatory.systemregistry.SystemRegistryClient;
import ai.aitia.arrowhead.application.spring.core.http.ServiceRegistryHTTPClient;

@Component
public class SystemRegistryWEBSOCKETClient { // JUST AN EXAMPLE

	//=================================================================================================
	// members
	
	private SystemRegistryClient client;
	
	@Autowired
	private ServiceRegistryHTTPClient serviceRegistry;
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public void init() {
		this.client = new SystemRegistryClient(new WebsocketCommunicator(), serviceRegistry.getClient());
	}
	
	//=================================================================================================
	// services
	
	//-------------------------------------------------------------------------------------------------
	public MonitoringService monitoringService() {
		return this.client.monitoringService();
	}
}
