package ai.aitia.arrowhead.application.spring.core.http;

import org.springframework.stereotype.Component;

import ai.aitia.arrowhead.application.common.networking.HttpsCommunicator;
import ai.aitia.arrowhead.application.common.networking.properties.HttpMethod;
import ai.aitia.arrowhead.application.common.service.MonitoringService;
import ai.aitia.arrowhead.application.core.mandatory.serviceregistry.ServiceRegistryClient;
import ai.aitia.arrowhead.application.core.mandatory.serviceregistry.service.ServiceDiscoveryService;

@Component
public class ServiceRegistryHTTPClient {

	//=================================================================================================
	// members
	
	private ServiceRegistryClient client;
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public void init(final String address, final int port, final String queryPath, final HttpMethod queryMethod) {
		this.client = new ServiceRegistryClient(new HttpsCommunicator(), address, port, queryPath, queryMethod);
		this.client.initialize();
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
