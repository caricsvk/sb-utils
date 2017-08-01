/**
 * Code Description
 211  System status, or system help reply.
 214  Help message.
 220  Domain service ready. Ready to start TLS.
 221  Domain service closing transmission channel.
 250  OK, queuing for node node started. Requested mail action okay, completed.
 251  OK, no messages waiting for node node. User not local, will forward to forwardpath.
 252  OK, pending messages for node node started. Cannot VRFY user (e.g., info is not local), but will take message for this user and attempt delivery.
 253  OK, messages pending messages for node node started.
 354  Start mail input; end with ..
 355  Octet-offset is the transaction offset.
 421  Domain service not available, closing transmission channel.
 432  A password transition is needed.
 450  Requested mail action not taken: mailbox unavailable. (ex. mailbox busy)
 451  Requested action aborted: local error in processing. Unable to process ATRN request now
 452  Requested action not taken: insufficient system storage.
 453  You have no mail.
 454  TLS not available due to temporary reason. Encryption required for requested authentication mechanism.
 458  Unable to queue messages for node node.
 459  Node node not allowed: reason.
 500  Command not recognized: command. Syntax error.
 501  Syntax error, no parameters allowed.
 502  Command not implemented.
 503  Bad sequence of commands.
 504  Command parameter not implemented.
 521  Machine does not accept mail.
 530  Must issue a STARTTLS command first. Encryption required for requested authentication mechanism.
 534  Authentication mechanism is too weak.
 538  Encryption required for requested authentication mechanism.
 550  Requested action not taken: mailbox unavailable.
 551  User not local; please try forwardpath.
 552  Requested mail action aborted: exceeded storage allocation.
 553  Requested action not taken: mailbox name not allowed.
 554  Transaction failed.
 */
package milo.utils.mail;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.naming.*;
import javax.naming.directory.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Basically taken and modified from rgagnon:
// See: http://www.rgagnon.com/javadetails/java-0452.html
// License: 
//    http://www.rgagnon.com/varia/faq-e.html#license
//    There is no restriction to use individual How-To in a development (compiled/source) but a mention is appreciated. 
public class MailBoxValidator {

	private static final Logger logger = LoggerFactory.getLogger(MailBoxValidator.class);

	private static final String[] domains = new String[]{"relowl.com", "gmail.com", "yahoo.com", "hotmail.com"
			, "seznam.cz", "azet.sk", "zoznam.sk", "stonline.sk", "atlas.sk", "atlas.cz"};
	private static final String[] prefixes = new String[]{"info", "kontakt", "contact", "infinity", "peter", "richard"};

	public static void main( String args[] ) {
//	private static void test() {
		Dummy[] testData = {
			new Dummy("", "nieuwsbrief@sokol.nl"),
			new Dummy("", "bla@bla.bla"),
			new Dummy("a", "caricsvk@gmail.com"),
			new Dummy("a", "richard.casar@hotmail.com")
		};

		int matches = 0;
		int falseMatches = 0;
		int trueMatches = 0;
		int falseMismatches = 0;
		int trueMismatches = 0;
		int nulls = 0;
		System.out.println("MailBoxValidator.main - start");
		for (int i = 0; i < testData.length; i++) {
			Boolean verified = verify(testData[i].email);
			boolean match = verified != null && testData[i].isValid == verified;
			String matchStr = match ? "match ": verified == null ? "NULL" : "MISMATCH";
			System.out.println(matchStr + " / " + testData[i].isValid + " / " + testData[i].email);
			if (match) {
				matches ++;
				if (verified) {
					trueMatches ++;
				} else {
					falseMatches ++;
				}
			} else if (Boolean.TRUE.equals(verified)) {
				trueMismatches ++;
			} else if (Boolean.FALSE.equals(verified)) {
				falseMismatches ++;
			} else {
				nulls ++;
			}
		}
		System.out.println("MailBoxValidator.main nulls / matches " + nulls + " / " + matches + " / " + testData.length
				+ ", true mis+matches " + trueMismatches + " + " + trueMatches
				+ ", false mis+matches " + falseMismatches + " + " + falseMatches);
		// 11 / 30 / 50, true mis+matches 4 + 18, false mis+matches 5 + 12
		// 12 / 32 / 50, true mis+matches 4 + 20, false mis+matches 2 + 12
		// 25 / 29 / 60, true mis+matches 2 + 19, false mis+matches 4 + 10
	}

	public static boolean isEmailSyntaxValid(String email) {
		return email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}$");
	}

	private static ArrayList<String> getMX(String hostName) throws NamingException {
		// Perform a DNS lookup for MX records in the domain
		Hashtable env = new Hashtable();
		env.put("java.naming.factory.initial",
				"com.sun.jndi.dns.DnsContextFactory");
		DirContext ictx = new InitialDirContext(env);
		Attributes attrs = ictx.getAttributes(hostName, new String[]{"MX"});
		Attribute attr = attrs.get("MX");

		// if we don't have an MX record, try the machine itself
		if ((attr == null) || (attr.size() == 0)) {
			attrs = ictx.getAttributes(hostName, new String[]{"A"});
			attr = attrs.get("A");
			if (attr == null)
				throw new NamingException("No match for name '" + hostName
						+ "'");
		}

		ArrayList<String> result = new ArrayList<>();
		NamingEnumeration en = attr.getAll();

		while (en.hasMore()) {
			String mailhost;
			String x = (String) en.next();
			String f[] = x.split(" ");
			// THE fix *************
			if (f.length == 1)
				mailhost = f[0];
			else if (f[1].endsWith("."))
				mailhost = f[1].substring(0, (f[1].length() - 1));
			else
				mailhost = f[1];
			// THE fix *************
			result.add(mailhost);
		}
		return result;
	}

	public static Boolean verify(String address) {

		if (!isEmailSyntaxValid(address)) {
			logger.info(address + " [mail validation] email is in invalid format");
			return false;
		}
		String domain = address.substring(address.indexOf('@') + 1);

		// Isolate the domain/machine name and get a list of mail exchangers
		ArrayList<String> mxList;
		try {
			mxList = getMX(domain);
		} catch (CommunicationException ce) {
			logger.info(address + " [mail validation] got dns problems" + ce.getMessage());
			return false;
		} catch (NamingException ex) {
			logger.info(address + " [mail validation] got host naming exception" + ex.getMessage());
			return false;
		}

		if (mxList.size() == 0) {
			logger.info(address + " [mail validation] no mx records");
			return false;
		}

		// modification, SMa: mx only use the first mx
		int mx = 0;
		BufferedReader rdr = null;
		BufferedWriter wtr = null;
		Socket socket = null;
		try {
			socket = new Socket();
			socket.setSoTimeout(10 * 1000);
			socket.connect(new InetSocketAddress(mxList.get(mx), 25), 10 * 1000);
			rdr = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			wtr = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

			int response = hear(rdr);
			if (response != 220) throw new Exception("Invalid header " + response);

			String fromEmail = getRandomFromEmail(address);
			String fromDomain = fromEmail.substring(fromEmail.indexOf('@') + 1);
			say(wtr, "EHLO " + fromDomain);

			response = hear(rdr);
			if (response != 250) throw new Exception("Not ESMTP " + response);

			// validate the sender address
			say(wtr, "MAIL FROM: <" + fromEmail + ">");
			response = hear(rdr);
			if (response != 250) throw new Exception("Sender rejected " + response);

			say(wtr, "RCPT TO: <" + address + ">");
			response = hear(rdr);

			// be polite
			say(wtr, "RSET");
			hear(rdr);
			say(wtr, "QUIT");
			hear(rdr);

			if (response == 250) {
				return true;
			}
			logger.info(address + " [mail validation] got response SMTP " + response);
		} catch (SocketTimeoutException ex) {
			logger.info(address + " mail validation] socket timeout. " + ex.getMessage());
			return false;
		} catch (Exception e) {
			logger.info(address + " [mail validation] remote mail validation error." + e.getMessage());
			return false;
		} finally {
			try {
				if (rdr != null) {
					rdr.close();
				}
				if (wtr != null) {
					wtr.close();
				}
				if (socket != null) {
					socket.close();
				}
			} catch (Exception ex) {
			}
		}
		return null;
	}

	private static int hear(BufferedReader in) throws IOException {
		String line;
		int res = 0;

		while ((line = in.readLine()) != null) {
			String pfx = line.substring(0, 3);
			try {
				res = Integer.parseInt(pfx);
			} catch (Exception ex) {
				res = -1;
			}
			if (line.charAt(3) != '-')
				break;
		}

		return res;
	}

	private static void say(BufferedWriter wr, String text) throws IOException {
		wr.write(text + "\r\n");
		wr.flush();
	}

	public static String getRandomFromEmail(String toEmail) {
		String toDomain = toEmail.substring(toEmail.indexOf('@') + 1);
		String domain;
		do {
			domain = domains[new Random().nextInt(domains.length)];
		} while (toDomain.equals(domain));
		return prefixes[new Random().nextInt(prefixes.length)] + "@" + domain;
	}

	static class Dummy {
		public boolean isValid;
		public String email;

		public Dummy(String isValid, String email) {
			this.isValid = isValid != null & !isValid.isEmpty();
			this.email = email;
		}
	}

}