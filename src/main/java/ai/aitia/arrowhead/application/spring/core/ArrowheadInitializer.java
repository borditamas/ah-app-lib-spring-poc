package ai.aitia.arrowhead.application.spring.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ai.aitia.arrowhead.application.common.networking.profile.CommunicationProfile;
import ai.aitia.arrowhead.application.common.networking.profile.InterfaceProfile;
import ai.aitia.arrowhead.application.common.networking.profile.Protocol;
import ai.aitia.arrowhead.application.common.networking.profile.http.HttpMethod;
import ai.aitia.arrowhead.application.common.networking.profile.http.HttpsKey;
import ai.aitia.arrowhead.application.common.service.MonitoringService;
import ai.aitia.arrowhead.application.common.verification.Ensure;
import ai.aitia.arrowhead.application.core.mandatory.serviceregistry.service.ServiceDiscoveryService;
import ai.aitia.arrowhead.application.core.mandatory.systemregistry.service.SystemDiscoveryService;
import ai.aitia.arrowhead.application.spring.core.client.DatamanagerClientBean;
import ai.aitia.arrowhead.application.spring.core.client.ServiceRegistryClientBean;
import ai.aitia.arrowhead.application.spring.core.client.SystemRegistryClientBean;
import ai.aitia.arrowhead.application.spring.networking.http.HttpsCommunicator;

@Component
public class ArrowheadInitializer {

	//=================================================================================================
	// members
	
	@Autowired
	private ServiceRegistryClientBean serviceRegistry;
	
	@Autowired
	private SystemRegistryClientBean systemRegistry;
	
	@Autowired
	private DatamanagerClientBean datamanager;
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public void initServiceRegistry(final CommunicationProfile communicationProfile, final InterfaceProfile queryInterfaceProfile) {
		serviceRegistry.initialize(communicationProfile, queryInterfaceProfile);
	}
	
	//-------------------------------------------------------------------------------------------------
	public void initSystemRegistry(final CommunicationProfile communicationProfile) {
		serviceRegistry.verifyInitialization();
		systemRegistry.initialize(communicationProfile);
	}
	
	//-------------------------------------------------------------------------------------------------
	public void initDatamanager(final CommunicationProfile communicationProfile) {
		serviceRegistry.verifyInitialization();
		datamanager.initialize(communicationProfile);
	}
	
	//-------------------------------------------------------------------------------------------------
	public void mandatoryHTTPS(final String serviceRegistryAddress, final int serviceRegistryPort, final String queryPath) {
		Ensure.notEmpty(serviceRegistryAddress, "serviceRegistryAddress is empty");
		Ensure.portRange(serviceRegistryPort);
		Ensure.notEmpty(queryPath, "queryPath is empty");
		
		final HttpsCommunicator httpsCommunicator = new HttpsCommunicator();
		httpsCommunicator.initialize();
		
		// Init Service Registry
		final CommunicationProfile sRCP = new CommunicationProfile();
		sRCP.put(ServiceDiscoveryService.NAME, httpsCommunicator);
		sRCP.put(MonitoringService.NAME, httpsCommunicator);
		final InterfaceProfile srQueryProfile = new InterfaceProfile(Protocol.HTTP);
		srQueryProfile.put(HttpsKey.METHOD, HttpMethod.POST);
		srQueryProfile.put(HttpsKey.ADDRESS, serviceRegistryAddress);
		srQueryProfile.put(HttpsKey.PORT, serviceRegistryPort);
		srQueryProfile.put(HttpsKey.PATH, queryPath);
		serviceRegistry.initialize(sRCP, srQueryProfile);
		
		// Init System Registry
		final CommunicationProfile sysRCP = new CommunicationProfile();
		sysRCP.put(MonitoringService.NAME, httpsCommunicator);
		sysRCP.put(SystemDiscoveryService.NAME, httpsCommunicator);
		systemRegistry.initialize(sysRCP);
		
		//TODO finish
	}
}
