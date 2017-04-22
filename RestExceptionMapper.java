package milo.utils;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class RestExceptionMapper implements ExceptionMapper<Throwable> {

	private static final Logger log = Logger.getLogger(RestExceptionMapper.class.getName());

	@Override
	public Response toResponse(Throwable exception) {
		log.log(Level.WARNING, "toResponse() caught exception", exception);
		return Response.status(getStatusCode(exception)).entity("Internal server error, check logs for more info.").build();
	}

	private int getStatusCode(Throwable exception) {
		if (exception instanceof WebApplicationException) {
			return ((WebApplicationException) exception).getResponse().getStatus();
		}
		return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
	}

}