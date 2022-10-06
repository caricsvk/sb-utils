package milo.utils.auth;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.xml.bind.DatatypeConverter;
import java.io.Serializable;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

@MappedSuperclass
@NamedQueries({
		@NamedQuery(
				name = AuthUser.FIND_BY_EMAIL,
				query = "SELECT entity FROM User entity WHERE entity.email = :email"
		),
		@NamedQuery(
				name = AuthUser.FIND_BY_CONFIRMATION_DATA,
				query = "SELECT entity FROM User entity WHERE entity.confirmationData = :confirmationData"
		),
})
public abstract class AuthUser implements Serializable {

	protected static Logger LOG = Logger.getLogger(AuthUser.class.getName());
	protected static final long serialVersionUID = 1L;

	public static final String FIND_BY_EMAIL = "User.findByEmail";
	public static final String FIND_BY_CONFIRMATION_DATA = "User.findByConfirmationData";

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	protected Long id;
	@NotNull @NotEmpty @Email
	@Column(unique = true)
	protected String email;

	@Enumerated(EnumType.STRING)
	protected ConfirmationStatusType confirmationStatus;
	protected String confirmationData;
	protected String password;
	protected String passwordRecovery;

	protected LocalDateTime created;
	protected LocalDateTime subscribed;

	public AuthUser() {
	}

	public AuthUser(Long id) {
		this.id = id;
	}

	public AuthUser(String email) {
		setEmail(email);
	}

	@PrePersist
	protected void prePersist() {
		created = LocalDateTime.now();
	}

	protected abstract String passwordHashSalt();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setCreated(LocalDateTime created) {
		this.created = created;
	}

	public LocalDateTime getCreated() {
		return created;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void setPlainPasswordOnPersistedUser(String plainPassword) {
		this.password = countPasswordHash(plainPassword);
	}

	public void setPlainRecoveryPassword(String plainPassword) {
		this.passwordRecovery = plainPassword == null ? null : countPasswordHash(plainPassword);
	}

	public boolean hasRecoveryPassword() {
		return this.passwordRecovery != null && !this.passwordRecovery.isBlank();
	}

	public static String generateRandomText() {
		String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
				+ "0123456789"
				+ "abcdefghijklmnopqrstuvxyz";
		int length = 17 + new Random().nextInt(6);
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			int index = (int)(AlphaNumericString.length() * Math.random());
			sb.append(AlphaNumericString.charAt(index));
		}
		return sb.toString();
	}

	public ConfirmationStatusType getConfirmationStatus() {
		return confirmationStatus;
	}

	public void setConfirmationStatus(ConfirmationStatusType confirmationStatus) {
		this.confirmationStatus = confirmationStatus;
	}

	public String getConfirmationData() {
		return confirmationData;
	}

	public void setConfirmationData(String confirmationData) {
		this.confirmationData = confirmationData;
	}

	public boolean isHisPassword(String password) {
		String passwordHashInput = countPasswordHash(password);
		return passwordHashInput.equals(this.password) || passwordHashInput.equals(this.passwordRecovery);
	}

	public boolean isDemoUser() {
		return getEmail() == null || getEmail().endsWith("@company.corp");
	}

	public void setSubscribed(LocalDateTime subscribed) {
		this.subscribed = subscribed;
	}

	public LocalDateTime getSubscribed() {
		return subscribed;
	}

	private String countPasswordHash(String text) {
		try {
			String dynamicSalt = this.getId().toString();
			String saltedText = dynamicSalt + passwordHashSalt() + text;
			MessageDigest messageDigest = MessageDigest.getInstance("MD5");
			messageDigest.update(saltedText.getBytes());
			byte[] digest = messageDigest.digest();
			return DatatypeConverter.printHexBinary(digest);
		} catch (Exception ex) {
			LOG.log(Level.SEVERE, "caught ex when hashing text ", ex);
			return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "errordigest" + text;
		}

	}

	public enum ConfirmationStatusType {
		SENT,
		SENDING_FAILED,
		EMAIL_VERIFICATION_FAILED,
		CONFIRMED
	}

	@Override
	public String toString() {
		return "User{" +
				"id=" + id +
				", email='" + email + '\'' +
				", confirmationStatus=" + confirmationStatus +
				", created=" + created +
				", subscribed=" + subscribed +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof AuthUser)) return false;
		AuthUser user = (AuthUser) o;
		return getId().equals(user.getId()) && getEmail().equals(user.getEmail()) && password.equals(user.password);
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId(), getEmail(), password);
	}

}
