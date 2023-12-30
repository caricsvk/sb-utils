package milo.utils.rest;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

public interface ReflectionRestApi {

	@GET
	@Path("entity-field-types")
	List<Field> types(@NotNull @QueryParam("fullClassName") String fullClassName) throws ClassNotFoundException;

	@GET
	@Path("enum")
	Object[] resolveEnum(@NotNull @QueryParam("fullClassName") String fullClassName);

	@GET
	@Path("is-enum")
	Map<String, Object> isEnum(@NotNull @QueryParam("fullClassName") String fullClassName);

}
