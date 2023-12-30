package milo.utils.auth;

import milo.utils.jpa.EntityService;

import jakarta.transaction.Transactional;
import java.util.List;

public abstract class AuthSessionsService<T extends AuthSession<U>, U extends AuthUser> extends EntityService<T, Long> {

	public AuthSessionsService(Class<T> entityClass) {
		super(entityClass, Long.class);
	}

	public abstract AuthSession<U> create(U user);

	@Override @Transactional
	public T find(Long id) {
		T session = super.find(id);
		if (session == null) {
			return null;
		}
		session.addFetch();
		return merge(session);
	}

	public List<T> findByUser(U user) {
		return getEntityManager()
				.createNamedQuery(AuthSession.FIND_BY_USER, entityClass)
				.setParameter("user", user)
				.getResultList();
	}
}
