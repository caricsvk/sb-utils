package milo.utils.auth;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.io.Serializable;
import java.time.LocalDateTime;

@MappedSuperclass
@NamedQueries({
		@NamedQuery(
				name = AuthSession.FIND_BY_USER,
				query = "SELECT entity FROM Session entity WHERE entity.user = :user"
		)
})
public abstract class AuthSession<T extends AuthUser> implements Serializable {

	private static final long serialVersionUID = 1L;
	public static final String FIND_BY_USER = "Session.findByUser";

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private Integer fetches = 0;
	private LocalDateTime created;
	private LocalDateTime updated;

	public AuthSession() {
	}

	@PrePersist
	private void prePersist() {
		created = LocalDateTime.now();
		this.preUpdate();
	}

	@PreUpdate
	private void preUpdate()  {
		updated = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public LocalDateTime getCreated() {
		return created;
	}

	public LocalDateTime getUpdated() {
		return updated;
	}

	public abstract T getUser();

	public abstract void setUser(T user);

	public Integer getFetches() {
		return fetches;
	}

	public void setFetches(Integer fetches) {
		this.fetches = fetches;
	}

	public void addFetch() {
		this.fetches ++;
	}
}
