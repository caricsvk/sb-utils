package milo.utils.rest;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

@Provider
public class CustomHeaderInterceptor implements WriterInterceptor {

	@Override
	public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
		Annotation[] annotations = context.getAnnotations();
		Stream.of(annotations).filter(annotation -> annotation instanceof CustomHeader).forEach(annotation ->
			context.getHeaders().add(((CustomHeader) annotation).key(), ((CustomHeader) annotation).value())
		);
		context.proceed();
	}
}
