package milo.utils.mail.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import milo.utils.mail.Email;
import milo.utils.mail.EmailAddress;
import milo.utils.mail.EmailAttachment;
import milo.utils.mail.EmailEvent;
import milo.utils.mail.EmailService;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Timestamp;
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
	protected abstract String getApiKey();

	@Override
	public String send(Email email) throws IOException {
		Client client = ClientBuilder.newClient();
		client.register(MultiPartFeature.class);
		client.register(HttpAuthenticationFeature.basic("api", getApiKey()));

		WebTarget target = client.target("https://api.mailgun.net/v3");
		FormDataMultiPart form = new FormDataMultiPart();
		String name = email.getSender().getName();
		form.field("subject", email.getSubject());
		form.field("from", (name == null ? "" : name) + "<" + email.getSender().getEmail() + ">");
		for (EmailAddress emailAddress : email.getRecipients()) {
			form.field("to",  makeRecipient(emailAddress));
		}
		for (EmailAddress emailAddress : email.getCopyRecipientsCc()) {
			form.field("cc",  makeRecipient(emailAddress));
		}
		for (EmailAddress emailAddress : email.getHiddenRecipientsBcc()) {
			form.field("bcc",  makeRecipient(emailAddress));
		}
		if (email.getReplyTo() != null) {
			form.field("h:Reply-To", makeRecipient(email.getReplyTo()));
		}
		if (email.getHtml() != null && !email.getHtml().isEmpty()) {
			form.field("html", email.getHtml());
		}
		if (email.getText() != null && !email.getText().isEmpty()) {
			form.field("text", email.getText());
		}
		for (Map.Entry<String, String> header : email.getHeaders().entrySet()) {
			form.field("h:" + header.getKey(), header.getValue());
		}
		for (EmailAttachment emailAttachment : email.getAttachments()) {
			try {
				InputStream attachmentInputStream = new URL(emailAttachment.getUrl()).openStream();
				form.bodyPart(new StreamDataBodyPart("attachment", attachmentInputStream, emailAttachment.getName(),
						MediaType.APPLICATION_OCTET_STREAM_TYPE));
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		if (email.getTxnId() == null || email.getTxnId().isEmpty()) {
			email.setTxnId(UUID.randomUUID().toString());
		}
		ObjectNode objectNode = OBJECT_MAPPER.createObjectNode();
		objectNode.put(INTERNAL_ID_KEY, email.getTxnId());
		form.field("v:" + INTERNAL_ID_KEY, OBJECT_MAPPER.writeValueAsString(objectNode));

		Response response = target.path("/{domain}/messages")
				.resolveTemplate("domain", getHost())
				.request(MediaType.MULTIPART_FORM_DATA_TYPE)
				.post(Entity.entity(form, form.getMediaType())); // MediaType.APPLICATION_FORM_URLENCODED

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
		LOGGER.info("got mailgun event: " + betterMap);
		EmailEvent emailEvent = new EmailEvent();
		emailEvent.setName(getValue("event", formData));
		String timestamp = getValue("timestamp", formData);
		if (timestamp != null) {
			emailEvent.setCreated(new Timestamp(Long.valueOf(timestamp) * 1000L));
		}
		emailEvent.setMetadata(betterMap);
		try {
			JsonNode jsonNode = OBJECT_MAPPER.readTree(getValue(INTERNAL_ID_KEY, formData));
			emailEvent.setEmailId(jsonNode.get(INTERNAL_ID_KEY).textValue());
		} catch (Exception ex) {
			LOGGER.log(Level.WARNING, "parsing txnId failed" + ex.getMessage(), ex);
		}
		return emailEvent;
	}

	private String getValue(String key, Map<String, String[]> formData) {
		return formData.containsKey(key) && formData.get(key).length > 0 ?
				formData.get(key)[0] : null;
	}

	private String makeRecipient(EmailAddress emailAddress) {
		return emailAddress.getName() + "<" + emailAddress.getEmail() + ">";
	}

}
