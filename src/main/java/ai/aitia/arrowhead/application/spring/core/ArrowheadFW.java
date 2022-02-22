package ai.aitia.arrowhead.application.spring.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ai.aitia.arrowhead.application.common.networking.profile.CommunicatorProfile;
import ai.aitia.arrowhead.application.common.networking.profile.InterfaceProfile;
import ai.aitia.arrowhead.application.spring.core.client.DatamanagerClientBean;
import ai.aitia.arrowhead.application.spring.core.client.ServiceRegistryClientBean;
import ai.aitia.arrowhead.application.spring.core.client.SystemRegistryClientBean;

@Component
public class ArrowheadFW {

	@Autowired
	private ServiceRegistryClientBean serviceRegistry;
	
	@Autowired
	private SystemRegistryClientBean systemRegistry;
	
	@Autowired
	private DatamanagerClientBean datamanager;
	
	public void initServiceRegistry(final CommunicatorProfile communicatorProfile, final InterfaceProfile queryInterfaceProfile) {
		serviceRegistry.initialize(communicatorProfile, queryInterfaceProfile);
	}
	
	public void initSystemRegistry(final CommunicatorProfile communicatorProfile) {
		serviceRegistry.getClient().verifyInitialization();
		systemRegistry.initialize(communicatorProfile);
	}
	
	public void initDatamanager(final CommunicatorProfile communicatorProfile) {
		serviceRegistry.getClient().verifyInitialization();
		datamanager.initialize(communicatorProfile);
	}
}
