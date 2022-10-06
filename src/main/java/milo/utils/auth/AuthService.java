package milo.utils.auth;

import milo.utils.jpa.EntityService;
import milo.utils.mail.MailBoxValidator;

import javax.persistence.NoResultException;
import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;
import javax.ws.rs.NotAuthorizedException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.logging.Logger;


public abstract class AuthService<T extends AuthUser> extends EntityService<T, Long> {

	private static Logger LOG = Logger.getLogger(AuthService.class.getName());
	public static final String userSessionKey = "user";

	public AuthService(Class<T> entityClass) {
		super(entityClass, Long.class);
	}

	protected abstract AuthSessionsService getSessionsService();
	protected abstract T newUser();
	protected abstract void sendPasswordRecovery(String email, String password);
	protected abstract boolean sendRegistrationConfirmation(
			String email, String password, String confirmationUUID, boolean subscribing
	);

	public AuthSession getLoggedSession(HttpSession httpSession) {
		try {
			AuthSession userSession = (AuthSession) httpSession.getAttribute(userSessionKey);
			AuthSession session = getSessionsService().find(userSession.getId());
			if (session != null) {
				return session;
			}
		} catch (Exception ex) {
			throw new NotAuthorizedException("session not found");
		}
		throw new NotAuthorizedException("user not found");
	}

	public T getLoggedUser(HttpSession httpSession) {
		AuthSession loggedSession = getLoggedSession(httpSession);
		return this.find(loggedSession.getUser().getId());
	}

	public T findUserByConfirmationData(String confirmationData) {
		try {
			return getEntityManager()
					.createNamedQuery(AuthUser.FIND_BY_CONFIRMATION_DATA, entityClass)
					.setParameter("confirmationData", confirmationData)
					.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

//	public T findUserByConfirmationData(String confirmationData) {
//		TableSearchQuery tableSearchQuery = new TableSearchQuery(null);
//		tableSearchQuery.setLimit(1);
//		tableSearchQuery.putEntityFilter(new EntityFilter(
//				"confirmationData", EntityFilterType.EXACT, Collections.singletonList(confirmationData)
//		));
//		List<T> result = this.search(tableSearchQuery);
//		return result.size() > 0 ? result.get(0) : null;
//	}

	public T findByEmailAndPlainPassword(String email, String password) {
		try {
			T user = this.findByEmail(email);
			if (user.isHisPassword(password)) {
				return user;
			}
		} catch (NoResultException e) {
			return null;
		}
		return null;
	}

	protected T findByEmail(String email) {
		return getEntityManager()
				.createNamedQuery(AuthUser.FIND_BY_EMAIL, entityClass)
				.setParameter("email", email)
				.getSingleResult();
	}

//	protected T findByEmail(String email) {
//		TableSearchQuery tableSearchQuery = new TableSearchQuery(null);
//		tableSearchQuery.setLimit(1);
//		tableSearchQuery.putEntityFilter(
//				new EntityFilter("email", EntityFilterType.EXACT, Collections.singletonList(email))
//		);
//		List<T> result = this.search(tableSearchQuery);
//		if (result.size() > 0) {
//			return result.get(0);
//		}
//		throw new NoResultException();
//	}

	public T getUserByEmail(String email) {
		try { // search for existing user
			return findByEmail(email);
		} catch (NoResultException e) {
			return newUser();
		}
	}

	@Transactional
	public T register(T user, boolean subscribing) {
		if (Boolean.FALSE.equals(MailBoxValidator.verify(user.getEmail()))) { // creating user but not sending email
			user.setConfirmationStatus(AuthUser.ConfirmationStatusType.EMAIL_VERIFICATION_FAILED);
		} else {
			String confirmationUUID = UUID.randomUUID().toString();
			String password = AuthUser.generateRandomText();

			boolean sent = this.sendRegistrationConfirmation(
					user.getEmail(), password, confirmationUUID, subscribing
			);
			if (user.getId() == null) {
				user = persist(user);
			}
			LOG.info("registering user " + user);
			user.setPlainPasswordOnPersistedUser(password);
			user.setConfirmationStatus(sent ? AuthUser.ConfirmationStatusType.SENT : AuthUser.ConfirmationStatusType.SENDING_FAILED);
			user.setConfirmationData(confirmationUUID);
		}
		user.setCreated(LocalDateTime.now());
		T mergedUser = merge(user);
		LOG.info("created user " + mergedUser);
		return mergedUser;
	}

	@Transactional
	public void recoverPassword(String email) {
		synchronized (AuthService.class) {
			try {
				T user = findByEmail(email);
				LOG.info("recoverPassword hasRecoveryPassword " + user.hasRecoveryPassword() + ", " + user);
				String password = AuthUser.generateRandomText();
				this.sendPasswordRecovery(user.getEmail(), password);
				user.setPlainRecoveryPassword(password);
				merge(user);
			} catch (NoResultException ex) {
				LOG.info("recoverPassword caught " + email);
			}
		}
	}

	@Transactional
	public void confirmUser(T user) {
		user.setConfirmationStatus(AuthUser.ConfirmationStatusType.CONFIRMED);
		user.setConfirmationData(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
		merge(user);
	}

	@Transactional
	public void subscribe(T entity) {
		entity.setSubscribed(LocalDateTime.now());
		merge(entity);
	}

}
