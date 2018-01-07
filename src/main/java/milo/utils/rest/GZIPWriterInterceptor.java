package milo.utils.rest;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

@Provider
public class GZIPWriterInterceptor implements WriterInterceptor {

	@Override
	public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
		context.getHeaders().add("Content-Encoding", "gzip");
		context.setOutputStream(new GZIPOutputStream(context.getOutputStream()));
		context.proceed();
	}
}