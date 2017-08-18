package milo.utils.mail;

import java.io.Serializable;

public interface EmailService {

	String send(Email email) throws Exception;
	EmailEvent parseEvent(Object object);

	CheckResult lightCheck(String recipientEmail);
	CheckResult expensiveCheck(String recipientEmail);

	class CheckResult implements Serializable {
		Boolean okay;
		String metadata;

		public CheckResult() {
		}

		public CheckResult(Boolean okay, String metadata) {
			this.okay = okay;
			this.metadata = metadata;
		}

		public Boolean getOkay() {
			return okay;
		}

		public void setOkay(Boolean okay) {
			this.okay = okay;
		}

		public String getMetadata() {
			return metadata;
		}

		public void setMetadata(String metadata) {
			this.metadata = metadata;
		}
	}
}
