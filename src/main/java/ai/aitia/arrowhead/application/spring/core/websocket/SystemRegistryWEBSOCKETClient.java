package ai.aitia.arrowhead.application.spring.core.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ai.aitia.arrowhead.application.common.service.MonitoringService;
import ai.aitia.arrowhead.application.core.mandatory.systemregistry.SystemRegistryClient;
import ai.aitia.arrowhead.application.spring.core.CoreClientBean;
import ai.aitia.arrowhead.application.spring.core.http.ServiceRegistryHTTPClient;
import ai.aitia.arrowhead.application.spring.networking.WebsocketCommunicator;

@Component
public class SystemRegistryWEBSOCKETClient implements CoreClientBean { // JUST AN EXAMPLE -> Datamanager Core Sys has websocket

	//=================================================================================================
	// members
	
	private SystemRegistryClient client;
	private boolean initialized = false;
	
	@Autowired
	private ServiceRegistryHTTPClient serviceRegistry;
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public void initialize() {
		this.client = new SystemRegistryClient(new WebsocketCommunicator(), serviceRegistry.getClient());
		client.initialize();
		this.initialized = true;
	}
	
	//-------------------------------------------------------------------------------------------------
	@Override
	public boolean isInitialized() {
		return this.initialized;
	}
	
	//=================================================================================================
	// services
	
	//-------------------------------------------------------------------------------------------------
	public MonitoringService monitoringService() {
		return this.client.monitoringService();
	}
}
