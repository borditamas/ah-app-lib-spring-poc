package ai.aitia.arrowhead.application.spring.networking.http.exception;

import org.springframework.http.HttpStatus;

import ai.aitia.arrowhead.application.common.verification.Ensure;

public class HttpResponseException extends Exception {
	
	private static final long serialVersionUID = -4247509128003658213L;
	
	private final HttpStatus status;

	public HttpResponseException(final HttpStatus status, final String message) {
		super(message);
		this.status = status;
		Ensure.notNull(status, "HttpStatus is null");
	}
	
	public HttpResponseException(final HttpStatus status, final String message, final Throwable throwable) {
		super(message, throwable);
		this.status = status;
		Ensure.notNull(status, "HttpStatus is null");
		Ensure.notNull(throwable, "Throwable is null");
	}

	public HttpStatus getStatus() { return status; }
}
