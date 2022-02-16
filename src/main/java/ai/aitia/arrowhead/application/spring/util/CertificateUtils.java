package ai.aitia.arrowhead.application.spring.util;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.ServiceConfigurationError;

import org.springframework.util.Assert;

public class CertificateUtils {
	
	//-------------------------------------------------------------------------------------------------
	public static X509Certificate getSystemCertFromKeyStore(final KeyStore keystore) {
		Assert.notNull(keystore, "Key store is not defined.");

        try {
            // the first certificate is not always the end certificate. java does not guarantee the order
			final Enumeration<String> enumeration = keystore.aliases();
            while (enumeration.hasMoreElements()) {
                final Certificate[] chain = keystore.getCertificateChain(enumeration.nextElement());

                if (Objects.nonNull(chain) && chain.length >= 3) {
                    return (X509Certificate) chain[0];
                }
            }
            throw new ServiceConfigurationError("Getting the first cert from keystore failed...");
        } catch (final KeyStoreException | NoSuchElementException ex) {
        	//TODO logger.error("Getting the first cert from key store failed...", ex);
            throw new ServiceConfigurationError("Getting the first cert from keystore failed...", ex);
        }
    }

	//=================================================================================================
	// assistant methods
	
	//-------------------------------------------------------------------------------------------------
	private CertificateUtils() {
		throw new UnsupportedOperationException();
	}
}
