package milo.utils.resource;

import java.time.LocalDateTime;
import java.util.logging.Logger;

public class CachedResponse<T> {

	private static final Logger LOG = Logger.getLogger(CachedResponse.class.getName());

	private LocalDateTime fetched;
	private int minutesExpiration = 20;
	protected T result;

	public CachedResponse() { }

	public CachedResponse(int minutesExpiration) {
		this.minutesExpiration = minutesExpiration;
	}

	public boolean isCurrent() {
		return fetched != null && fetched.isAfter(LocalDateTime.now().minusMinutes(minutesExpiration));
	}

	public T getResult() {
		return result;
	}

	public void setResultUpdateFetched(T result) {
		this.fetched = LocalDateTime.now();
		this.result = result;
	}

	public void setExpiration(int minutes) {
		this.minutesExpiration = minutes;
	}

	public void clear() {
//		LOG.info("clearing cache fetched at " + fetched + "; minutes expiration: " + minutesExpiration);
		this.fetched = null;
		this.result = null;
	}

	public LocalDateTime getFetched() {
		return fetched;
	}

	public void setFetched(LocalDateTime fetched) {
		this.fetched = fetched;
	}
}
