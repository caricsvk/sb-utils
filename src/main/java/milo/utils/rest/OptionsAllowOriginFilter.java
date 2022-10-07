package milo.utils.rest;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
public class OptionsAllowOriginFilter implements ContainerResponseFilter {

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
		if ("OPTIONS".equals(requestContext.getMethod())) {
			responseContext.getHeaders().putSingle("Access-Control-Allow-Origin", "*");
			responseContext.getHeaders().putSingle("Access-Control-Allow-Headers", "*");
//			responseContext.getHeaders().add("Allow", String.join(", ", responseContext.getAllowedMethods()));
		}
	}

}
