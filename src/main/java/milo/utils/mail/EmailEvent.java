package milo.utils.mail;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

public class EmailEvent {

	private String name;
	private String emailId;
	private Timestamp created;
	private Map<String, List<String>> metadata;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmailId() {
		return emailId;
	}

	public void setEmailId(String emailId) {
		this.emailId = emailId;
	}

	public Timestamp getCreated() {
		return created;
	}

	public void setCreated(Timestamp created) {
		this.created = created;
	}

	public Map<String, List<String>> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, List<String>> metadata) {
		this.metadata = metadata;
	}

	@Override
	public String toString() {
		return "EmailEvent{" +
				"name='" + name + '\'' +
				", emailId='" + emailId + '\'' +
				", created=" + created +
				", metadata=" + metadata +
				'}';
	}
}
