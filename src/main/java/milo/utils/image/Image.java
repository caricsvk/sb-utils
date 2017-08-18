package milo.utils.image;

import java.awt.image.BufferedImage;

public class Image implements ImageInfo {

	private String name;
	private int height;
	private int width;
	private String url;
	private byte[] content;
	private String contentType;
	private BufferedImage bufferedImage;

	public Image(String url, int width, int height) {
		this.height = height;
		this.width = width;
		this.url = url;
	}

	public Image() {
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public int getResolution() {
		return height * width;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public byte[] getContent() {
		return content;
	}

	public void setContent(byte[] content) {
		this.content = content;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public BufferedImage getBufferedImage() {
		return bufferedImage;
	}

	public void setBufferedImage(BufferedImage bufferedImage) {
		this.bufferedImage = bufferedImage;
	}
}
