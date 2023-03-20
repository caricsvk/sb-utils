package milo.utils.rest;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Provider
public class OptionsAllowOriginFilter implements ContainerResponseFilter {

	public static List<String> credentialsOriginsPrefixes = new ArrayList();

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
		if ("OPTIONS".equals(requestContext.getMethod())) {
			String requestedOrigin = requestContext.getHeaderString("origin");
			if (requestedOrigin == null) {
				requestedOrigin = "";
			}
			boolean allowedOrigin = credentialsOriginsPrefixes.stream().anyMatch(requestedOrigin::startsWith);
			responseContext.getHeaders().putSingle("Access-Control-Allow-Credentials", "true");
			responseContext.getHeaders().putSingle("Access-Control-Allow-Origin", allowedOrigin ? requestedOrigin : "*");
			responseContext.getHeaders().add("Access-Control-Allow-Methods",
					String.join(", ", responseContext.getAllowedMethods())
			);
			responseContext.getHeaders().putSingle("Access-Control-Allow-Headers",
					"Content-Type, Accept, Cookie," +
					"prevent-default-error-handling, exclude-from-counting, prevent-default-cancellation"
			);
		}
	}

}
