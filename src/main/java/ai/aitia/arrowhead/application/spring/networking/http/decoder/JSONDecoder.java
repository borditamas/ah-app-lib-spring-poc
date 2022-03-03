package ai.aitia.arrowhead.application.spring.networking.http.decoder;

import ai.aitia.arrowhead.application.common.networking.decoder.MediaType;
import ai.aitia.arrowhead.application.common.networking.decoder.PayloadDecoder;

public class JSONDecoder implements PayloadDecoder {

	@Override
	public <T> T decode(final MediaType media, byte[] payload, Class<T> type) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T decode(final MediaType media, String payload, Class<T> type) throws Exception {
		return null;
	}

}
