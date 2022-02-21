package ai.aitia.arrowhead.application.spring.networking.http;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.el.MethodNotFoundException;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.security.auth.x500.X500Principal;

import org.apache.http.HttpHeaders;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import ai.aitia.arrowhead.application.common.exception.CommunicationException;
import ai.aitia.arrowhead.application.common.exception.DeveloperException;
import ai.aitia.arrowhead.application.common.networking.CommunicationClient;
import ai.aitia.arrowhead.application.common.networking.CommunicationProperties;
import ai.aitia.arrowhead.application.common.networking.profile.InterfaceProfile;
import ai.aitia.arrowhead.application.common.networking.profile.Protocol;
import ai.aitia.arrowhead.application.common.networking.profile.http.HttpsKey;
import ai.aitia.arrowhead.application.common.networking.profile.model.QueryParams;
import ai.aitia.arrowhead.application.common.verification.Ensure;
import ai.aitia.arrowhead.application.spring.networking.exception.HttpResponseException;

public class HttpsClient implements CommunicationClient {
	
	//=================================================================================================
	// members
	
	private final String clientName;
	private final CommunicationProperties props;
	private final SSLContext sslContext;
	private final int connectionTimeout;
	private final int socketTimeout;
	private final int connectionManagerTimeout;
	private final InterfaceProfile interfaceProfile;
		
	private final RestTemplate sslTemplate;	
	private ResponseEntity<Object> response = null;
	
	private static final String ERROR_MESSAGE_PART_PKIX_PATH = "PKIX path";
	private static final String ERROR_MESSAGE_PART_SUBJECT_ALTERNATIVE_NAMES = "doesn't match any of the subject alternative names";	
	private static final List<HttpMethod> NOT_SUPPORTED_METHODS = List.of(HttpMethod.HEAD, HttpMethod.OPTIONS, HttpMethod.TRACE); 

	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	
	public HttpsClient(final String clientName, final CommunicationProperties props, final SSLContext sslContext, final int connectionTimeout,
					   final int socketTimeout, final int connectionManagerTimeout, final InterfaceProfile interfaceProfile) {
		this.clientName = clientName;
		this.props = props;
		this.sslContext = sslContext;
		this.connectionTimeout = connectionTimeout;
		this.socketTimeout = socketTimeout;
		this.connectionManagerTimeout = connectionManagerTimeout;
		
		Ensure.notNull(this.props, "CommunicationProperties is null");
		Ensure.notNull(interfaceProfile, "interfaceProfile is null");
		Ensure.isTrue(interfaceProfile.getProtocol() == Protocol.HTTP, "Invalid protocol for HttpsClient: " + interfaceProfile.getProtocol().name());
		Ensure.notEmpty(interfaceProfile.getAddress(), "address is empty");
		Ensure.portRange(interfaceProfile.getPort());
		Ensure.isTrue(interfaceProfile.contains(HttpsKey.METHOD), "No http method defined");
		this.interfaceProfile = interfaceProfile;
		this.sslTemplate = createTemplate(this.sslContext);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Override
	public void send(final Object payload) throws CommunicationException {
		send(null, payload);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Override
	public void send(final QueryParams params, final Object payload) throws CommunicationException {
		if (this.response != null) {
			throw new CommunicationException("Previous response was not read received.");
		}
		
		try {
			this.response = sendRequest(interfaceProfile, Object.class, params, payload);			
				
		} catch (final DeveloperException ex) {
			throw ex;
			
		} catch (final HttpResponseException ex) {
			throw new CommunicationException(ex.getMessage(), ex);
				
		} catch (final Exception ex) {
			// log
			throw new CommunicationException(ex.getMessage());
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Override
	public <T>T receive(final Class<T> type) throws CommunicationException {
		final Object body = this.response.getBody();
		this.response = null;
		if (body == null) {
			return null;
		}
		return (T)body;
	}
	
	//=================================================================================================
	// assistant methods
	
	//-------------------------------------------------------------------------------------------------
	private <T,P> ResponseEntity<T> sendRequest(final InterfaceProfile interfaceProfile, final Class<T> responseType, final QueryParams params, final P payload) throws HttpResponseException {
		Ensure.notNull(responseType, "responseType is null");
	
		final HttpMethod method = HttpMethod.valueOf(interfaceProfile.get(ai.aitia.arrowhead.application.common.networking.profile.http.HttpMethod.class, HttpsKey.METHOD).name());
		if (NOT_SUPPORTED_METHODS.contains(method)) {
			throw new MethodNotFoundException("Invalid method type was given to the HttpService.sendRequest() method.");
		}
		
		final UriComponents uri = createURI(interfaceProfile.getAddress(), interfaceProfile.getPort(), params, interfaceProfile.get(String.class, HttpsKey.PATH));
		
		final HttpEntity<P> entity = getHttpEntity(payload);
		try {
			return sslTemplate.exchange(uri.toUri(), method, entity, responseType);
		} catch (final ResourceAccessException ex) {
			if (ex.getMessage().contains(ERROR_MESSAGE_PART_PKIX_PATH)) {
//				logger.error("The system at {} is not part of the same certificate chain of trust!", uri.toUriString());
				throw new HttpResponseException(HttpStatus.UNAUTHORIZED, "The system at " + uri.toUriString() + " is not part of the same certificate chain of trust!");
			} else if (ex.getMessage().contains(ERROR_MESSAGE_PART_SUBJECT_ALTERNATIVE_NAMES)) {
//				logger.error("The certificate of the system at {} does not contain the specified IP address or DNS name as a Subject Alternative Name.", uri.toString());
				throw new HttpResponseException(HttpStatus.UNAUTHORIZED, "The certificate of the system at " + uri.toString() + " does not contain the specified IP address or DNS name as a Subject Alternative Name.");
			} else {
//		        logger.error("UnavailableServerException occurred at {}", uri.toUriString());
//		        logger.debug("Exception", ex);
				throw new HttpResponseException(HttpStatus.SERVICE_UNAVAILABLE, "Could not get any response from: " + uri.toUriString(), ex);
			}
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	private RestTemplate createTemplate(final SSLContext sslContext) {
		final HttpClient client = createClientInternal(sslContext);
		final HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(client) {
			// This modification is needed to reuse already established connections in subsequent HTTP calls.
			
			@Override
			protected HttpContext createHttpContext(final HttpMethod httpMethod, final URI uri) {
				final HttpContext context = new HttpClientContext(new BasicHttpContext());
				if (clientName != null && !clientName.isBlank()) {
					context.setAttribute(HttpClientContext.USER_TOKEN, new X500Principal(clientName));
				}
				
				return context;
			}
		};
		final RestTemplate restTemplate = new RestTemplate(factory);
		//TODO restTemplate.setErrorHandler(errorHandler);
		return restTemplate;
	}
	
	//-------------------------------------------------------------------------------------------------
	private HttpClient createClientInternal(final SSLContext sslContext) {
		HttpClient client;
		
		if (sslContext == null) {
			client = HttpClients.custom().setDefaultRequestConfig(createRequestConfig())
										 .build();
		} else {
			SSLConnectionSocketFactory socketFactory;
			if (this.props.isDisableHostnameVerifier()) { // just for testing, DO NOT USE this in a production environment
				final HostnameVerifier allHostsAreAllowed = (hostname, session) -> true; 
				socketFactory = new SSLConnectionSocketFactory(sslContext, allHostsAreAllowed);
			} else {
				socketFactory = new SSLConnectionSocketFactory(sslContext);
			}
			client = HttpClients.custom().setDefaultRequestConfig(createRequestConfig())
										 .setSSLSocketFactory(socketFactory)
										 .build();
		}
		
		return client;
	}
	
	//-------------------------------------------------------------------------------------------------
	private RequestConfig createRequestConfig() {
		return RequestConfig.custom().setConnectTimeout(this.connectionTimeout)
									 .setSocketTimeout(this.socketTimeout)
									 .setConnectionRequestTimeout(this.connectionManagerTimeout)
									 .build();
	}
	
	//-------------------------------------------------------------------------------------------------
	private UriComponents createURI(final String host, final int port, final QueryParams queryParams, final String path) {
		final UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
		builder.scheme(Protocol.HTTP.name())
			   .host(host.trim())
			   .port(port);
		
		if (path != null && !path.isBlank()) {
			builder.path(path);
		}
		
		if (queryParams != null && queryParams.getParams().size() != 0) {
			if (queryParams.getParams().size() % 2 != 0) {
				//TODO throw new InvalidParameterException("queryParams variable arguments conatins a key without value");
			}
			
			final LinkedMultiValueMap<String, String> query = new LinkedMultiValueMap<>();
			int count = 1;
			String key = "";
			for (final String vararg : queryParams.getParams()) {
				if (count % 2 != 0) {
					query.putIfAbsent(vararg, new ArrayList<>());
					key = vararg;
				} else {
					query.get(key).add(vararg);
				}
				count++;
			}
			builder.queryParams(query);			
		}

		return builder.build();
	}
	
	//-------------------------------------------------------------------------------------------------
	private <P> HttpEntity<P> getHttpEntity(final P payload) {
		final MultiValueMap<String,String> headers = new LinkedMultiValueMap<>();
		headers.put(HttpHeaders.ACCEPT, Arrays.asList(MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_JSON_VALUE));
		if (payload != null) {
			headers.put(HttpHeaders.CONTENT_TYPE, Collections.singletonList(MediaType.APPLICATION_JSON_VALUE));
		}
		
		return payload != null ? new HttpEntity<>(payload, headers) : new HttpEntity<>(headers);
	}
}
