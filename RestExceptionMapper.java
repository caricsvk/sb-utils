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
		if (exception instanceof WebApplicationException) {
			WebApplicationException webApplicationException = (WebApplicationException) exception;
			Response response = webApplicationException.getResponse();
			log.log(Level.INFO, "RestExceptionMapper caught exception at " + getUrl() + " : " + exception.getMessage());
			// log only not expected exceptions
			if (response.getStatus() < 400 || response.getStatus() > 406) {
				log.log(Level.WARNING, exception.getMessage(), exception);
			}
			return Response.status(response.getStatus())
					.entity(response.getStatusInfo().toString()).build();
		} else {
			log.log(Level.WARNING, "RestExceptionMapper caught exception at " + getUrl() + " : "
					+ exception.getMessage(), exception);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Internal server error, check logs for more info.").build();
		}
	}

	private String getUrl() {
		return request != null ? request.getRequestURI() : "Unknown URI";
	}

}