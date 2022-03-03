package ai.aitia.arrowhead.application.spring.networking.http;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;

import org.apache.http.ssl.SSLContextBuilder;

import ai.aitia.arrowhead.application.common.exception.InitializationException;
import ai.aitia.arrowhead.application.common.networking.CommunicationClient;
import ai.aitia.arrowhead.application.common.networking.CommunicationProperties;
import ai.aitia.arrowhead.application.common.networking.Communicator;
import ai.aitia.arrowhead.application.common.networking.CommunicatorType;
import ai.aitia.arrowhead.application.common.networking.decoder.PayloadDecoder;
import ai.aitia.arrowhead.application.common.networking.profile.InterfaceProfile;
import ai.aitia.arrowhead.application.common.verification.Ensure;
import ai.aitia.arrowhead.application.spring.util.CertificateUtils;

public class HttpsCommunicator implements Communicator {
	
	//=================================================================================================
	// members
	
	private CommunicationProperties props;
	private String clientName;
	private SSLContext sslContext;
	private PayloadDecoder decoder;
	
	private int connectionTimeout = 30000;
	private int socketTimeout = 30000;
	private int connectionManagerTimeout = 10000;

	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public int getConnectionTimeout() { return connectionTimeout; }	
	public int getSocketTimeout() { return socketTimeout; }	
	public int getConnectionManagerTimeout() { return connectionManagerTimeout; }	
	
	//-------------------------------------------------------------------------------------------------
	public void setConnectionTimeout(final int connectionTimeout) { this.connectionTimeout = connectionTimeout; }	
	public void setSocketTimeout(final int socketTimeout) { this.socketTimeout = socketTimeout; }
	public void setConnectionManagerTimeout(final int connectionManagerTimeout) { this.connectionManagerTimeout = connectionManagerTimeout; }
	
	//-------------------------------------------------------------------------------------------------
	@Override
	public CommunicatorType type() {
		return CommunicatorType.HTTPS;
	}
	
	//-------------------------------------------------------------------------------------------------
	@Override
	public void properties(final CommunicationProperties props) {
		Ensure.notNull(props, "CommunicationProperties is null");
		this.props = props;		
	}
	
	//-------------------------------------------------------------------------------------------------
	@Override
	public void decoder(final PayloadDecoder decoder) {
		Ensure.notNull(decoder, "PayloadDecoder is null");
		this.decoder = decoder;		
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void initialize() {
		Ensure.notNull(props, "CommunicationProperties is null");
		try {
			createSSLContext();
		} catch (final KeyManagementException | UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException ex) {
			throw new InitializationException(ex.getMessage(), ex);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public boolean isInitialized() {
		return this.sslContext != null && this.decoder != null;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public CommunicationClient client(final InterfaceProfile interfaceProfile) {
		if (!isInitialized()) {
			throw new InitializationException("HttpsCommunicator is not initialized");
		}
		return new HttpsClient(this.clientName, this.props, this.sslContext, this.connectionTimeout, this.socketTimeout, this.connectionManagerTimeout, interfaceProfile, this.decoder);
	}
	
	//=================================================================================================
	// assistant methods
	
	//-------------------------------------------------------------------------------------------------
	private void createSSLContext() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, KeyManagementException, UnrecoverableKeyException {
		Ensure.notNull(this.props, "CommunicationProperties is null");
		final KeyStore keyStore = KeyStore.getInstance(this.props.getKeyStoreType());
		keyStore.load(new BufferedInputStream(Files.newInputStream(this.props.getKeyStorePath(), new OpenOption[0])), this.props.getKeyPassword().toCharArray());
		
		final X509Certificate certFromKeystore = CertificateUtils.getSystemCertFromKeyStore(keyStore);
		this.clientName = certFromKeystore.getSubjectDN().getName();		
		
		this.sslContext = new SSLContextBuilder().loadTrustMaterial(new File(this.props.getTrustStorePath().toUri()), this.props.getTrustStorePassword().toCharArray())
										   		 .loadKeyMaterial(keyStore, this.props.getKeyPassword().toCharArray())
										   		 .setKeyStoreType(this.props.getKeyStoreType())
										   		 .build();
	}
}
