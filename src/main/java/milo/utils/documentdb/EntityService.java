package milo.utils.documentdb;

import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.PathParam;
import java.util.List;

public abstract class EntityService<T extends DocumentEntity> {

	private final Class<T> entityClass;

	protected abstract DocumentManager getDocumentManager();

	public EntityService(Class<T> entityClass) {
		this.entityClass = entityClass;
	}

	public T persist(@Valid T entity) {
		String id = getDocumentManager().insert(entity);
		entity.setId(id);
		return entity;
	}

	public T merge(@Valid T entity) {
		getDocumentManager().update(entity);
		return entity;
	}

	public T find(@PathParam("id") String id) {
		return getDocumentManager().get(entityClass, id);
	}

	public void remove(@PathParam("id") String id) {
		getDocumentManager().delete(entityClass, id);
	}

	public long count(@BeanParam DocumentSearchQuery dsr) {
		return getDocumentManager().findCount(entityClass, dsr);
	}

	public List<T> getAll(@BeanParam DocumentSearchQuery dsr) {
		return getDocumentManager().find(entityClass, dsr);
	}

}
