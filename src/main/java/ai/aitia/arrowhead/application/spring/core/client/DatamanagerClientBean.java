package ai.aitia.arrowhead.application.spring.core.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ai.aitia.arrowhead.application.common.networking.profile.CommunicationProfile;
import ai.aitia.arrowhead.application.common.service.MonitoringService;
import ai.aitia.arrowhead.application.core.support.datamanager.DatamanagerClient;
import ai.aitia.arrowhead.application.core.support.datamanager.service.HistorianService;
import ai.aitia.arrowhead.application.spring.core.CoreClientBean;

@Component
public class DatamanagerClientBean implements CoreClientBean {

	//=================================================================================================
	// members
	
	private DatamanagerClient client;
	
	@Autowired
	private ServiceRegistryClientBean serviceRegistry;
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public void initialize(final CommunicationProfile communicatorProfile) {
		this.client = new DatamanagerClient(communicatorProfile, serviceRegistry.getClient().serviceDiscoveryService());
		client.initialize();
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public boolean isInitialized() {
		serviceRegistry.getClient().serviceDiscoveryService().verify();
		return this.client.isInitialized();
	}
	
	//-------------------------------------------------------------------------------------------------
	@Override
	public void verifyInitialization() {
		this.client.verifyInitialization();
	}
	
	//=================================================================================================
	// services
	
	//-------------------------------------------------------------------------------------------------
	public MonitoringService monitoringService() {
		return this.client.monitoringService();
	}
	
	//-------------------------------------------------------------------------------------------------
	public HistorianService historianService() {
		return this.client.historianService();
	}
}
