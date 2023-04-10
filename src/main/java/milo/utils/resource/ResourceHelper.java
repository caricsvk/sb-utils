package milo.utils.resource;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ResourceHelper {

	public static String hashOrOriginal(String key) {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("MD5");
			messageDigest.update(key.getBytes());
			byte[] digest = messageDigest.digest();
			return DatatypeConverter.printHexBinary(digest);
		} catch (NoSuchAlgorithmException e) {
			System.out.println("MD5 algorithm not available: " + e.getMessage());
			return key;
		}
	}

}

