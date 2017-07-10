package milo.utils.mail;

public interface EmailService {

	String send(Email email) throws Exception;
	EmailEvent parseEvent(Object object);

}
