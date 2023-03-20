package milo.utils.rest;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Provider @PreMatching
public class OptionsAllowOriginFilter implements ContainerRequestFilter {

	public static List<String> credentialsOriginsPrefixes = new ArrayList();

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		if ("OPTIONS".equals(requestContext.getMethod())) {
			String requestedOrigin = requestContext.getHeaderString("origin");
			if (requestedOrigin == null) {
				requestedOrigin = "";
			}
			boolean allowedOrigin = credentialsOriginsPrefixes.stream().anyMatch(requestedOrigin::startsWith);
			Response response = Response.ok()
					.header("Access-Control-Allow-Credentials", "true")
					.header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
					.header("Access-Control-Allow-Origin", allowedOrigin ? requestedOrigin : "*")
					.header("Access-Control-Allow-Headers", "Content-Type, Accept, Cookie, " +
							"prevent-default-error-handling, exclude-from-counting, prevent-default-cancellation")
					.build();
			requestContext.abortWith(response);
		}
	}
}
