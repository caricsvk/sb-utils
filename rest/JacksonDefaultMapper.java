package milo.utils.rest;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Named;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

@Named
@Provider
public class JacksonDefaultMapper implements ContextResolver<ObjectMapper> {

	public static ObjectMapper provider = new ObjectMapper();

	{
		provider.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		provider.findAndRegisterModules();
	}

	@Override
	public ObjectMapper getContext(Class<?> type) {
		return provider;
	}
}