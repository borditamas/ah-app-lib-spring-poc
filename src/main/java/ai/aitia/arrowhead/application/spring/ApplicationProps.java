package ai.aitia.arrowhead.application.spring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ai.aitia.arrowhead.application.common.networking.CommunicationProperties;

@Component
public class ApplicationProps {

	//=================================================================================================
	// members
	
	public static final String KEYSTORE_TYPE = "server.ssl.key-store-type:PKCS12";
	public static final String KEYSTORE_PATH = "server.ssl.key-store";
	public static final String KEYSTORE_PASSWORD = "server.ssl.key-store-password";
	public static final String KEY_PASSWORD = "server.ssl.key-password";
	public static final String TRUSTSTORE_PATH = "server.ssl.trust-store";
	public static final String TRUSTSTORE_PASSWORD = "server.ssl.trust-store-password";
	public static final String DISABLE_HOSTNAME_VERIFIER = "disable.hostname.verifier:false";
	
	@Value(KEYSTORE_TYPE)
	private String keyStoreType;
	
	@Value(KEYSTORE_PATH)
	private String keyStorePath;
	
	@Value(KEYSTORE_PASSWORD)
	private String keyStorePassword;
	
	@Value(KEY_PASSWORD)
	private String keyPassword;
	
	@Value(TRUSTSTORE_PATH)
	private String trustStorePath;
	
	@Value(TRUSTSTORE_PASSWORD)
	private String trustStorePassword;
	
	@Value(DISABLE_HOSTNAME_VERIFIER)
	private boolean disableHostnameVerifier;
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public String getKeyStoreType() { return keyStoreType; }
	public String getKeyStorePath() { return keyStorePath; }
	public String getKeyStorePassword() { return keyStorePassword; }
	public String getKeyPassword() { return keyPassword; }
	public String getTrustStorePath() { return trustStorePath; }
	public String getTrustStorePassword() { return trustStorePassword; }
	public boolean isDisableHostnameVerifier() { return disableHostnameVerifier; }
	
	//-------------------------------------------------------------------------------------------------
	public CommunicationProperties getCommunicationProperties() {
		return new CommunicationProperties(keyStoreType, keyStorePassword, keyPassword, keyStorePath, trustStorePassword, trustStorePath);
	}
}
