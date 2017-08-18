package milo.utils.eventstore.exceptions;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

public class AggregateAccessException extends RuntimeException {

	private String message;
	private Response.Status status;

	public AggregateAccessException(String message) {
		super();
		this.message = message;
	}

	public AggregateAccessException setUnavailable() {
		status = Response.Status.SERVICE_UNAVAILABLE;
		return this;
	}

	public AggregateAccessException setConflict() {
		status = Response.Status.CONFLICT;
		return this;
	}

	public AggregateAccessException setNotFound() {
		status = Response.Status.NOT_FOUND;
		return this;
	}

	@Override
	public String getMessage() {
		if (super.getMessage() != null) {
			message += ". " + super.getMessage();
		}
		return message;
	}

	@Provider
	public static class AggregateAccessExMapper implements ExceptionMapper<AggregateAccessException> {

		@Override
		public Response toResponse(AggregateAccessException ex) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(ex.getMessage()).build();
		}
	}

}
