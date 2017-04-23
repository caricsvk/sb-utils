package milo.utils.mail;

import java.util.Arrays;

public class EmailAttachment {

	String url;
	String name;
	byte[] data;
	String type;

	public EmailAttachment() {
	}

	/**
	 *
	 * @param url
	 */
	public EmailAttachment(String url) {
		this.url = url;
	}

	/**
	 *
	 * @param name
	 * @param data
	 * @param type
	 */
	public EmailAttachment(String name, byte[] data, String type) {
		this.name = name;
		this.data = data;
		this.type = type;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getName() {
		return name == null || name.isEmpty() ? url.substring(url.lastIndexOf('/') + 1) : name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return "Attachement{" +
				"url='" + url + '\'' +
				", name='" + name + '\'' +
				", data=" + Arrays.toString(data) +
				", type='" + type + '\'' +
				'}';
	}
}
