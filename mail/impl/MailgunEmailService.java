package milo.utils.mail.impl;

import milo.utils.mail.Email;
import milo.utils.mail.EmailAddress;
import milo.utils.mail.EmailAttachment;
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
import java.util.Map;

public abstract class MailgunEmailService implements EmailService {

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

		Response response = target.path("/{domain}/messages")
				.resolveTemplate("domain", getHost())
				.request(MediaType.MULTIPART_FORM_DATA_TYPE)
				.post(Entity.entity(form, form.getMediaType())); // MediaType.APPLICATION_FORM_URLENCODED
		System.out.println(response);
		return response.readEntity(String.class);
	}

	private String makeRecipient(EmailAddress emailAddress) {
		return emailAddress.getName() + "<" + emailAddress.getEmail() + ">";
	}

}
