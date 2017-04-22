package milo.utils.rest;

import milo.utils.jpa.EntityService;
import milo.utils.jpa.search.TableSearchQuery;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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