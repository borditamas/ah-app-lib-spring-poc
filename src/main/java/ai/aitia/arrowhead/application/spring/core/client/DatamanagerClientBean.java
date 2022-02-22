package ai.aitia.arrowhead.application.spring.core.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ai.aitia.arrowhead.application.common.networking.profile.CommunicatorProfile;
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
	public void initialize(final CommunicatorProfile communicatorProfile) {
		this.client = new DatamanagerClient(communicatorProfile, serviceRegistry.getClient());
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
	
	//-------------------------------------------------------------------------------------------------
	public HistorianService historianService() {
		return this.client.historianService();
	}
}
