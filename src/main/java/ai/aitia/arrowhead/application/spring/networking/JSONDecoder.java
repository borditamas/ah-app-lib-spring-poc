package ai.aitia.arrowhead.application.spring.networking;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.aitia.arrowhead.application.common.networking.decoder.MediaType;
import ai.aitia.arrowhead.application.common.networking.decoder.PayloadDecoder;
import ai.aitia.arrowhead.application.common.networking.decoder.exception.PayloadDecodingException;
import ai.aitia.arrowhead.application.common.verification.Ensure;

public class JSONDecoder implements PayloadDecoder {

	//=================================================================================================
	// members
	
	private final ObjectMapper mapper = new ObjectMapper();
	
	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public <T> T decode(final MediaType media, final byte[] payload, final Class<T> type) throws Exception {
		Ensure.notNull(media, "media is null");
		Ensure.notEmpty(payload, "payload is empty");
		Ensure.notNull(type, "type is null");
		
		switch (media) {
		case EMPTY:
			return null;
			
		case TEXT:
		case JSON:
			return mapper.readValue(payload, type);

		default:
			throw new PayloadDecodingException("Unknown media type: " + media.name());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public <T> T decode(final MediaType media, final String payload, final Class<T> type) throws Exception {
		Ensure.notNull(media, "media is null");
		Ensure.notEmpty(payload, "payload is empty");
		Ensure.notNull(type, "type is null");
		
		switch (media) {
		case EMPTY:
			return null;
			
		case TEXT:
		case JSON:
			return mapper.readValue(payload, type);

		default:
			throw new PayloadDecodingException("Unknown media type" + media.name());
		}
	}

}
