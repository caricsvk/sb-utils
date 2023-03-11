package milo.utils.resource;

import java.time.LocalDateTime;

public class CachedResponse<T> {

	LocalDateTime fetched;
	int minutesExpiration = 20;
	T result;

	public CachedResponse(LocalDateTime fetched) {
		this.fetched = fetched;
	}

	public CachedResponse(LocalDateTime fetched, int minutesExpiration) {
		this.fetched = fetched;
		this.minutesExpiration = minutesExpiration;
	}

	boolean isCurrent() {
		return fetched != null && fetched.isAfter(LocalDateTime.now().minusMinutes(minutesExpiration));
	}
}
