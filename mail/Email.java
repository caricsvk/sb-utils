package milo.utils.mail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Email {

	private EmailAddress sender;
	private EmailAddress replyTo;
	private List<EmailAddress> recipients;
	private List<EmailAddress> copyRecipientsCc;
	private List<EmailAddress> hiddenRecipientsBcc;
	private String subject;
	private String html;
	private String text;
	private List<EmailAttachment> attachments;
	private List<EmailAttachment> images;
	private Map<String, String> headers;

	public Email() {
	}

	public EmailAddress getSender() {
		return sender;
	}

	public void setSender(EmailAddress sender) {
		this.sender = sender;
	}

	public EmailAddress getReplyTo() {
		return replyTo;
	}

	public void setReplyTo(EmailAddress replyTo) {
		this.replyTo = replyTo;
	}

	public List<EmailAddress> getRecipients() {
		if (recipients == null) {
			recipients = new ArrayList<>();
		}
		return recipients;
	}

	public void setRecipients(List<EmailAddress> recipients) {
		this.recipients = recipients;
	}

	public void addRecipient(EmailAddress emailAddress) {
		getRecipients().add(emailAddress);
	}

	public void addRecipient(String email, String name) {
		getRecipients().add(new EmailAddress(email, name));
	}

	public List<EmailAddress> getCopyRecipientsCc() {
		if (copyRecipientsCc == null) {
			copyRecipientsCc = new ArrayList<>();
		}
		return copyRecipientsCc;
	}

	public void setCopyRecipientsCc(List<EmailAddress> copyRecipientsCc) {
		this.copyRecipientsCc = copyRecipientsCc;
	}

	public void addCopyRecipientCc(EmailAddress copyRecipient) {
		getCopyRecipientsCc().add(copyRecipient);
	}

	public List<EmailAddress> getHiddenRecipientsBcc() {
		if (hiddenRecipientsBcc == null) {
			hiddenRecipientsBcc = new ArrayList<>();
		}
		return hiddenRecipientsBcc;
	}

	public void setHiddenRecipientsBcc(List<EmailAddress> hiddenRecipientsBcc) {
		this.hiddenRecipientsBcc = hiddenRecipientsBcc;
	}

	public void addHiddenRecipientBcc(EmailAddress emailAddress) {
		getHiddenRecipientsBcc().add(emailAddress);
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getHtml() {
		return html;
	}

	public void setHtml(String html) {
		this.html = html;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public List<EmailAttachment> getAttachments() {
		if (attachments == null) {
			attachments = new ArrayList<>();
		}
		return attachments;
	}

	public void setAttachments(List<EmailAttachment> attachments) {
		this.attachments = attachments;
	}

	public void addAttachment(EmailAttachment attachment) {
		getAttachments().add(attachment);
	}

	public List<EmailAttachment> getImages() {
		if (images == null) {
			images = new ArrayList<>();
		}
		return images;
	}

	public void setImages(List<EmailAttachment> images) {
		this.images = images;
	}

	public void addImage(EmailAttachment attachment) {
		getImages().add(attachment);
	}

	public Map<String, String> getHeaders() {
		if (headers == null) {
			headers = new HashMap<>();
		}
		return headers;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	@Override
	public String toString() {
		return "Email{" +
				"sender=" + sender +
				", replyTo=" + replyTo +
				", recipients=" + recipients +
				", copyRecipientsCc=" + copyRecipientsCc +
				", hiddenRecipientsBcc=" + hiddenRecipientsBcc +
				", subject='" + subject + '\'' +
				", html='" + html + '\'' +
				", text='" + text + '\'' +
				", attachments=" + attachments +
				", images=" + images +
				", headers=" + headers +
				'}';
	}
}
