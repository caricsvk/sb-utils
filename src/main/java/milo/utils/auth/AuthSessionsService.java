package milo.utils.auth;

import milo.utils.jpa.EntityService;

import javax.transaction.Transactional;
import java.util.List;

public abstract class AuthSessionsService<T extends AuthSession, U extends AuthUser> extends EntityService<T, Long> {

	public AuthSessionsService(Class<T> entityClass) {
		super(entityClass, Long.class);
	}

	protected abstract AuthService getAuthService();
	public abstract AuthSession create(U user);

	@Override @Transactional
	public T find(Long id) {
		T session = super.find(id);
		if (session == null) {
			return null;
		}
		session.addFetch();
		return merge(session);
	}

	public List<T> findByUser(AuthUser user) {
		return getEntityManager()
				.createNamedQuery(AuthSession.FIND_BY_USER, entityClass)
				.setParameter("user", user)
				.getResultList();
	}
}