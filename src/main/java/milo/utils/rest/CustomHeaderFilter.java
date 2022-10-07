package milo.utils.rest;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.stream.Stream;

@Provider
public class CustomHeaderFilter implements ContainerResponseFilter {

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
		addToHeadersByCustomHeaderAnnotation(responseContext.getHeaders(), responseContext.getEntityAnnotations());
	}

	public static void addToHeadersByCustomHeaderAnnotation(
			MultivaluedMap<String, Object> headers, Annotation[] annotations
	) {
		Stream.of(annotations).filter(annotation -> annotation instanceof CustomHeader).forEach(annotation ->
				headers.add(((CustomHeader) annotation).key(), ((CustomHeader) annotation).value())
		);
	}

}
