package milo.utils.rest;

import milo.utils.jpa.EntityService;
import milo.utils.jpa.search.TableSearchQuery;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import java.util.List;

public interface EntityRestApi<T, ID> {

	@POST
	T create(@NotNull @Valid T entity);

	@PUT
	T update(@NotNull @Valid T entity);

	@DELETE
	@Path("{id}")
	void delete(@NotNull @PathParam("id") ID id);

	@GET
	@Path("{id}")
	T read(@NotNull @PathParam("id") ID id);

	@GET
	@Path("count")
	EntityService.NumericValue count(@BeanParam TableSearchQuery tableSearchQuery);

	@GET
	List<T> read(@BeanParam TableSearchQuery tableSearchQuery) throws InterruptedException;

}
