package milo.utils.mail.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import milo.utils.mail.Email;
import milo.utils.mail.EmailAddress;
import milo.utils.mail.EmailEvent;
import milo.utils.mail.EmailService;
import milo.utils.mail.MailBoxValidator;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class MailgunEmailService implements EmailService {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final Logger LOGGER = Logger.getLogger(MailgunEmailService.class.getName());
	private static final String INTERNAL_ID_KEY = "internal-message-id";

	protected abstract String getHost();

	protected abstract Client getClient();
//	{
//		if (client == null) {
//			client = ClientBuilder.newClient();
//			client.register(MultiPartFeature.class);
//			client.register(HttpAuthenticationFeature.basic("api", getApiKey()));
//		}
//		return client;
//	}

	public List<Bounce> fetchBounces() {
		WebTarget target = getClient().target("https://api.mailgun.net/v3");
		Response response = target.path("/{domain}/bounces").resolveTemplate("domain", getHost()).request().get();
		String responseMessage = response.readEntity(String.class);
		List<Bounce> result = new ArrayList<>();
		if (response.getStatus() >= 200 && response.getStatus() < 300) {
			try {
				JsonNode responseMessageObject = OBJECT_MAPPER.readTree(responseMessage);
				JsonNode items = responseMessageObject.get("items");
				for (int i = 0; i < items.size(); i ++) {
					JsonNode item = items.get(i);
					result.add(new Bounce(item.get("address").textValue(), item.get("error").textValue()));
				}

			} catch (Exception ex) {
				LOGGER.log(Level.WARNING, "caught Exception by parsing bounces response: " + ex.getMessage(), ex);
			}
		} else {
			LOGGER.log(Level.WARNING, "error fetching bounces " + responseMessage);
		}
		return result;
	}

	public int deleteBounce(Bounce bounce) {
		WebTarget target = getClient().target("https://api.mailgun.net/v3");
		Response response = target.path("/{domain}/bounces/{address}")
				.resolveTemplate("domain", getHost())
				.resolveTemplate("address", bounce.getAddress())
				.request().delete();
		String responseMessage = response.readEntity(String.class);
		LOGGER.info(responseMessage);
		return response.getStatus();
	}

	@Override
	public String send(Email email) throws IOException {
		WebTarget target = getClient().target("https://api.mailgun.net/v3");
		Form form = new Form();
		String name = email.getSender().getName();
		form.param("subject", email.getSubject());
		form.param("from", (name == null ? "" : name) + "<" + email.getSender().getEmail() + ">");
		for (EmailAddress emailAddress : email.getRecipients()) {
			form.param("to",  makeRecipient(emailAddress));
		}
		for (EmailAddress emailAddress : email.getCopyRecipientsCc()) {
			form.param("cc",  makeRecipient(emailAddress));
		}
		for (EmailAddress emailAddress : email.getHiddenRecipientsBcc()) {
			form.param("bcc",  makeRecipient(emailAddress));
		}
		if (email.getReplyTo() != null) {
			form.param("h:Reply-To", makeRecipient(email.getReplyTo()));
		}
		if (email.getHtml() != null && !email.getHtml().isEmpty()) {
			form.param("html", email.getHtml());
		}
		if (email.getText() != null && !email.getText().isEmpty()) {
			form.param("text", email.getText());
		}
		for (Map.Entry<String, String> header : email.getHeaders().entrySet()) {
			form.param("h:" + header.getKey(), header.getValue());
		}
		// TODO
//		for (EmailAttachment emailAttachment : email.getAttachments()) {
//			try {
//				InputStream attachmentInputStream = new URL(emailAttachment.getUrl()).openStream();
//				form.bodyPart(new StreamDataBodyPart("attachment", attachmentInputStream, emailAttachment.getName(),
//						MediaType.APPLICATION_OCTET_STREAM_TYPE));
//			} catch (IOException ex) {
//				ex.printStackTrace();
//			}
//		}
		if (email.getTxnId() == null || email.getTxnId().isEmpty()) {
			email.setTxnId(UUID.randomUUID().toString());
		}
		ObjectNode objectNode = OBJECT_MAPPER.createObjectNode();
		objectNode.put(INTERNAL_ID_KEY, email.getTxnId());
		form.param("v:" + INTERNAL_ID_KEY, OBJECT_MAPPER.writeValueAsString(objectNode));

		Response response = target.path("/{domain}/messages")
				.resolveTemplate("domain", getHost())
				.request(MediaType.MULTIPART_FORM_DATA_TYPE)
				.post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED)); // form.getMediaType()

		LOGGER.info(response.toString());
		String responseMessage = response.readEntity(String.class);
		if (response.getStatus() >= 200 && response.getStatus() < 300) {
//			try {
//				JsonNode responseMessageObject = objectMapper.readTree(responseMessage);
//				email.setTxnId(responseMessageObject.get("id").textValue().replace("<", "").replace(">", ""));
//				responseMessage = responseMessageObject.get("message").textValue();
//			} catch (Exception ex) {
//				LOGGER.log(Level.WARNING, "caught Exception by parsing send e-mail response: " + ex.getMessage(), ex);
//			}
		} else {
			LOGGER.info(email.toString());
		}
		return responseMessage;
	}

	@Override
	public EmailEvent parseEvent(Object object) {
		Map<String, String[]> formData = (Map<String, String[]>) object;
		Map<String, List<String>> betterMap = new HashMap<>();
		for (String key : formData.keySet()) {
			betterMap.put(key, Arrays.asList(formData.get(key)));
		}
		LOGGER.info("got mailgun event: " + betterMap + " : " + formData.keySet());
		EmailEvent emailEvent = new EmailEvent();
		emailEvent.setName(getValue("event", formData));
		if (formData.keySet().size() == 0 || emailEvent.getName() == null) {
			return null;
		}
		String timestamp = getValue("timestamp", formData);
		if (timestamp != null) {
			emailEvent.setCreated(new Timestamp(Long.valueOf(timestamp) * 1000L));
		}
		emailEvent.setMetadata(betterMap);
		try {
			JsonNode jsonNode = OBJECT_MAPPER.readTree(getValue(INTERNAL_ID_KEY, formData));
			emailEvent.setEmailId(jsonNode.get(INTERNAL_ID_KEY).textValue());
		} catch (Exception ex) {
			LOGGER.log(Level.WARNING, "parsing txnId failed" + ex.getMessage());
		}
		return emailEvent;
	}

	@Override
	public CheckResult lightCheck(String recipientEmail) {
		CheckResult checkResult = new CheckResult();
		Boolean verify = MailBoxValidator.verify(recipientEmail);
		checkResult.setOkay(verify);
		checkResult.setMetadata(MailBoxValidator.getLastLog());
		return checkResult;
	}

	@Override
	public CheckResult expensiveCheck(String recipientEmail) {
		throw new RuntimeException("not implemented yet");
	}

	private String getValue(String key, Map<String, String[]> formData) {
		return formData.containsKey(key) && formData.get(key).length > 0 ?
				formData.get(key)[0] : null;
	}

	private String makeRecipient(EmailAddress emailAddress) {
		return (emailAddress.getName() == null ? "" : emailAddress.getName()) + "<" + emailAddress.getEmail() + ">";
	}

	public static class Bounce {
		private String address;
		private String reason;

		public Bounce(String address, String reason) {
			this.address = address;
			this.reason = reason;
		}

		public String getAddress() {
			return address;
		}

		public String getReason() {
			return reason;
		}
	}

}
