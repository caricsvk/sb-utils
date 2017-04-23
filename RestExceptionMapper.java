package milo.utils;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class RestExceptionMapper implements ExceptionMapper<Throwable> {

	@Context
	private HttpServletRequest request;

	private static final Logger log = Logger.getLogger(RestExceptionMapper.class.getName());

	@Override
	public Response toResponse(Throwable exception) {
		log.log(Level.WARNING, "RestExceptionMapper caught exception at " + getUrl() + " : "
				+ exception.getMessage(), exception);
		return Response.status(getStatusCode(exception)).entity(getResponseBody(exception)).build();
	}

	private int getStatusCode(Throwable exception) {
		if (exception instanceof WebApplicationException) {
			return ((WebApplicationException) exception).getResponse().getStatus();
		}
		return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
	}

	private String getResponseBody(Throwable exception) {
		if (exception instanceof WebApplicationException) {
			return ((WebApplicationException) exception).getResponse().getStatusInfo().toString();
		}
		return "Internal server error, check logs for more info.";
	}

	private String getUrl() {
		return request != null ? request.getRequestURI() : "Unknown URI";
	}

}