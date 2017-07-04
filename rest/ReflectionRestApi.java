package milo.utils.rest;

import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

public interface ReflectionRestApi {

	@GET
	@Path("entity-field-types")
	Map<String, String> types(@NotNull @QueryParam("fullClassName") String fullClassName) throws ClassNotFoundException;

	@GET
	@Path("enum")
	Object[] resolveEnum(@NotNull @QueryParam("fullClassName") String fullClassName);

	@GET
	@Path("is-enum")
	Map<String, Object> isEnum(@NotNull @QueryParam("fullClassName") String fullClassName);

}