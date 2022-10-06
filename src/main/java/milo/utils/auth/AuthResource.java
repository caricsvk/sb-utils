package milo.utils.auth;

import milo.utils.mail.MailBoxValidator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

public abstract class AuthResource {

	private static Logger LOG = Logger.getLogger(AuthResource.class.getName());

	protected abstract AuthService<AuthUser> getAuthService();
	protected abstract AuthSessionsService getSessionsService();
	protected abstract HttpServletRequest getHttpServletRequest();

	@GET
	public AuthUser getLoggedUser() {
		LOG.info("getLoggedUser");
		try {
			HttpSession session = getHttpServletRequest().getSession(false);
//			System.out.println("UsersResource.getLoggedUser " + session.getMaxInactiveInterval());
//			session.setMaxInactiveInterval(60*60*24*30);
			return getAuthService().getLoggedSession(session).getUser();
		} catch (NotAuthorizedException ex) {
			return null;
		}
	}

	@DELETE
	public void logout() {
		HttpSession session = getHttpServletRequest().getSession(false);
		if (session != null) {
			session.invalidate();
		}
	}

	@POST @Path("login")
	public Response login(@NotNull @Valid UserInput userInput) {
		AuthUser user = this.getAuthService().findByEmailAndPlainPassword(userInput.getEmail(), userInput.getPassword());
		if (user == null) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
		// confirming by logging in
		if (!AuthUser.ConfirmationStatusType.CONFIRMED.equals(user.getConfirmationStatus())) {
			this.getAuthService().confirmUser(user);
		}
		this.setPasswordAndDiscardRecovery(user, userInput.password);
		this.createLoggedSession(user.getId());
		return Response.ok(user).build();
	}

	@POST @Path("register")
	public Response register(@QueryParam("email") @NotNull @NotEmpty @Email String email) {
		AuthUser user = getAuthService().getUserByEmail(email);
		if (user.getId() != null) {
			LOG.info("user already registered " + user);
		}

		if (user.getConfirmationStatus() != null) {
			return Response.status(Response.Status.CONFLICT).build();
		} else if (! MailBoxValidator.isEmailSyntaxValid(email)) {
			return Response.status(422).build();
		}

		getAuthService().register(user, false);
		return Response.ok().build();
	}

	@PUT @Path("password")
	public Response updatePassword(@NotNull @Valid Passwords passwords) {
		AuthUser loggedUser = getLoggedUser();
		if (loggedUser == null) {
			throw new NotAuthorizedException("");
		}
		AuthUser user = this.getAuthService().findByEmailAndPlainPassword(loggedUser.getEmail(), passwords.getOriginal());
		if (user == null) {
			throw new BadRequestException();
		}
		this.setPasswordAndDiscardRecovery(user, passwords.getNewPassword());
		return Response.ok().build();
	}

	@POST @Path("password-recovery")
	public void recoverPassword(@QueryParam("email") @NotNull @NotBlank String email) {
		getAuthService().recoverPassword(email);
	}

	@GET @Path("register/{key}")
	public Response confirm(@PathParam("key") @NotEmpty String key) {
		AuthUser user = getAuthService().findUserByConfirmationData(key);
		LOG.info("confirming user " + user + " by key " + key);
		if (user != null) {
			this.getAuthService().confirmUser(user);
			// create session when actually confirming URL first time
			this.createLoggedSession(user.getId());
		}
		return Response.ok().build();
	}

	private void setPasswordAndDiscardRecovery(AuthUser user, String newPassword) {
		// discard recovery password, keep only just used/set password
		user.setPlainPasswordOnPersistedUser(newPassword);
		user.setPlainRecoveryPassword(null);
		getAuthService().merge(user);
	}

	private void createLoggedSession(Long userId) {
		AuthSession session = getSessionsService().create(this.getAuthService().find(userId));
		getHttpServletRequest().getSession(true).setAttribute(
				AuthService.userSessionKey, session
		);
	}

	public static class UserInput {
		@NotNull @NotEmpty @Email
		private String email;
		@NotNull @NotEmpty
		private String password;

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}
	}

	public static class Passwords {
		@NotNull @NotEmpty
		private String original;
		@NotNull @NotEmpty
		private String newPassword;

		public String getOriginal() {
			return original;
		}

		public void setOriginal(String original) {
			this.original = original;
		}

		public String getNewPassword() {
			return newPassword;
		}

		public void setNewPassword(String newPassword) {
			this.newPassword = newPassword;
		}
	}

}
