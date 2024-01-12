package milo.utils.auth;

import jakarta.servlet.http.HttpServletRequest;
import milo.utils.jpa.EntityService;
import milo.utils.mail.MailBoxValidator;

import jakarta.persistence.NoResultException;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.NotAuthorizedException;
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

	protected abstract AuthSessionsService<AuthSession<T>, T> getSessionsService();
	protected abstract T createNewUser();
	protected abstract void sendPasswordRecovery(String email, String password);
	protected abstract boolean sendRegistrationConfirmation(
			String email, String password, String confirmationUUID, boolean subscribing
	);

	public AuthSession<T> getLoggedSession(HttpServletRequest httpServletRequest) {
		return getLoggedSession(httpServletRequest.getSession(false));
	}

	public AuthSession<T> getLoggedSession(HttpSession httpSession) {
		try {
			AuthSession<T> userSession = (AuthSession<T>) httpSession.getAttribute(userSessionKey);
			AuthSession<T> session = getSessionsService().find(userSession.getId());
			if (session != null) {
				return session;
			}
		} catch (Exception ex) {
			throw new NotAuthorizedException("session not found");
		}
		throw new NotAuthorizedException("user not found");
	}

	public T getLoggedUser(HttpServletRequest httpServletRequest) {
		return getLoggedUser(httpServletRequest.getSession(false));
	}

	public T getLoggedUser(HttpSession httpSession) {
		AuthSession<T> loggedSession = getLoggedSession(httpSession);
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
			T newUser = createNewUser();
			newUser.setEmail(email);
			return newUser;
		}
	}

	@Transactional
	public T register(@Valid T user, boolean subscribing) {
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
