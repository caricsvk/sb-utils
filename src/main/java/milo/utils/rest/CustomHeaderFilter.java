package milo.utils.rest;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Provider
public class CustomHeaderFilter implements ContainerResponseFilter {

	public static final String ALLOW_CREDENTIALS = "xyAllowCredentials!";
	public static List<String> credentialsOriginsPrefixes = new ArrayList();

	@Context private ResourceInfo resourceInfo;

	@Override
	public void filter(
			ContainerRequestContext requestContext,
			ContainerResponseContext responseContext
	) throws IOException {
		String requestedOrigin = requestContext.getHeaderString("origin");
		if (requestedOrigin == null) {
			requestedOrigin = "";
		}
		boolean allowedOrigin = credentialsOriginsPrefixes.stream().anyMatch(requestedOrigin::startsWith);
		addToHeadersByCustomHeaderAnnotation(
				responseContext.getHeaders(),
				resourceInfo.getResourceMethod() != null ?
						resourceInfo.getResourceMethod().getAnnotations() : responseContext.getEntityAnnotations(),
				allowedOrigin ? requestedOrigin : ""
		);
	}

	public static void addToHeadersByCustomHeaderAnnotation(
			MultivaluedMap<String, Object> headers, Annotation[] annotations, String origin
	) {
		List<CustomHeader> singleCustomHeader = Stream.of(annotations)
				.filter(annotation -> annotation instanceof CustomHeader)
				.map(annotation -> (CustomHeader) annotation)
				.collect(Collectors.toList());
		List<CustomHeader> multiCustomHeaders = Stream.of(annotations)
				.filter(annotation -> annotation instanceof CustomHeaders)
				.flatMap(annotation -> Stream.of(((CustomHeaders) annotation).value()))
				.collect(Collectors.toList());
		multiCustomHeaders.addAll(singleCustomHeader);
		multiCustomHeaders.forEach(annotation -> addHeader(headers, annotation, origin));
	}

	private static void addHeader(MultivaluedMap<String, Object> headers, CustomHeader annotation, String origin) {
		if (ALLOW_CREDENTIALS.equals(annotation.key()) || ALLOW_CREDENTIALS.equals(annotation.value())) {
			headers.add("Access-Control-Allow-Origin", origin);
			headers.add("Access-Control-Allow-Credentials", "true");
		} else {
			headers.add(annotation.key(), annotation.value());
		}
	}

}
