package milo.utils.image;

import milo.utils.HttpHelper;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class ImageHelper {

	private static Logger LOG = Logger.getLogger(ImageHelper.class.getName());

	/**
	 * @param images
	 * @return img info with biggest available image if exists otherwise null
	 */
	public static ImageInfo findBiggestSimilarByUrl(List<ImageInfo> images, int minWidth) {
		try {
			return images.stream().filter(i -> i.getWidth() > minWidth).sorted(
					(t0, t1) -> t1.getWidth() * t1.getHeight() - t0.getWidth() * t0.getWidth()
			).findFirst().get();
		} catch (NoSuchElementException ex) {
			return null;
		}
	}

	public static List<ImageInfo> findSimilarByUrl(String url) {
		List<ImageInfo> images = new ArrayList<>();
		try {
			String response = HttpHelper.download("https://images.google.com/searchbyimage?image_url=" +
					URLEncoder.encode(url, "UTF-8")).getContent();
			Matcher urlMatcher = Pattern.compile("th _.+?<a.+?imgurl=(.*?)&amp;imgrefurl", Pattern.DOTALL)
					.matcher(response);
			Matcher resolutionMatcher = Pattern
					.compile("class=\"f\">\\s*?([0-9]+[^<]*?[0-9]+)[^<]*?</span>", Pattern.DOTALL)
					.matcher(response);
			while (urlMatcher.find() && resolutionMatcher.find()) {
				String[] resoulutions = resolutionMatcher.group(1).split("-")[0].split(" ");
				int width = new Integer(resoulutions[0].trim()).intValue();
				int height = new Integer(resoulutions[2].trim()).intValue();
				images.add(new Image(urlMatcher.group(1), width, height));
			}
			return images;
		} catch (Exception ex) {
			LOG.log(Level.WARNING, ex.getMessage(), ex);
			return images;
		}
	}

	public static Image createFromUrl(String url) {
		return createFromUrl(url, null, null);
	}

	/**
	 *
	 * @param url
	 * @param connectionTimeoutMs
	 * @param readTimeoutSeconds
	 * @return
	 */
	public static Image createFromUrl(String url, Integer connectionTimeoutMs, Integer readTimeoutSeconds) {

		if (! HttpHelper.isUrl(url)) {
			return null;
		}
		Image image;
		HttpURLConnection urlConnection = null;
		try {

			urlConnection = HttpHelper.buildUrlConnection(url);

			if (connectionTimeoutMs != null) {
				urlConnection.setConnectTimeout(connectionTimeoutMs);
			}
			if (readTimeoutSeconds != null) {
				urlConnection.setReadTimeout(readTimeoutSeconds);
			}

			urlConnection.connect();

			String imgName = "not-defined";
			try {
				String[] splitedUrl = url.split("/");
				imgName = splitedUrl[splitedUrl.length - 1];
				imgName = imgName.split("\\?")[0];
			} catch (Exception e) {
				LOG.log(Level.SEVERE, "MediaResource.createFromUrl caught " + e.getMessage() + " for " + url);
			}

			InputStream inputStream;
			if (urlConnection.getContentEncoding() != null && urlConnection.getContentEncoding().equals("gzip")) {
				inputStream = new GZIPInputStream(urlConnection.getInputStream());
			} else {
				inputStream = urlConnection.getInputStream();
			}
			image = create(inputStream, imgName, urlConnection.getContentType(), url);
		} catch (Exception ex) {
			LOG.log(Level.WARNING, "caught X ception createFromUrl: " + ex.getMessage() + "; for url: " + url);
			return null;
		} finally {
			if (urlConnection != null) {
				urlConnection.disconnect();
			}
		}
		return image.getContent() == null || image.getContent().length == 0 ? null : image;
	}

	/**
	 *
	 * @param inputStream
	 * @param name
	 * @param contentType
	 * @param url - optional
	 * @return
	 * @throws IOException
	 */
	public static Image create(InputStream inputStream, String name, String contentType, String url) throws IOException {
		Image image = new Image();
		BufferedImage ioImage = ImageIO.read(inputStream);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			inputStream.close();
			String formatName = extractContentTypeForImageIO(contentType, name);
			ImageIO.write(ioImage, formatName, outputStream);
			image.setContentType("image/" + formatName);
		} catch (Exception ex) {
			String alternativeFormatName = "png";
			ImageIO.write(ioImage, alternativeFormatName, outputStream);
			image.setContentType("image/" + alternativeFormatName);
		}
		outputStream.flush();
		outputStream.close();
		image.setName(name);
		image.setUrl(url);
		image.setHeight(ioImage.getHeight());
		image.setWidth(ioImage.getWidth());
		image.setContent(outputStream.toByteArray());
		image.setBufferedImage(ioImage);
		return image;
	}

	public static BufferedImage getScaledInstance(BufferedImage image, int newWidth, int scaleType) {
		int newHeight = (int) ((float) newWidth / image.getWidth() * image.getHeight());
		BufferedImage newImage = new BufferedImage(newWidth, newHeight, image.getType());
		newImage.getGraphics().drawImage(
				image.getScaledInstance(newWidth, -1, scaleType), 0, 0, null);
		return newImage;
	}

	public static byte[] getImageBytes(BufferedImage newImage, String contentType) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			ImageIO.write(newImage, extractContentTypeForImageIO(contentType), outputStream);
			outputStream.flush();
			byte[] result = outputStream.toByteArray();
			outputStream.close();
			return result;
		} catch (Exception ex) {
			return null;
		}
	}

	private static String extractContentTypeForImageIO(String contentType) {
		return contentType != null && contentType.startsWith("image") ?
				contentType.split("/")[1].split(";")[0] : "jpeg";
	}

	private static String extractContentTypeForImageIO(String contentType, String filename) {
		String ending = "";
		if (filename != null) {
			String[] splitImgName = filename.split("\\.");
			ending = splitImgName[splitImgName.length - 1];
		}
		return contentType != null && contentType.startsWith("image") ? contentType.split("/")[1].split(";")[0]
				: "jpeg".equals(ending) || "jpg".equals(ending) || "png".equals(ending) ? ending : "jpeg";
	}

	public static byte[] bestResize(byte[] data, int width, String contentType) throws IOException {
		BufferedImage originalInstance = ImageIO.read(new ByteArrayInputStream(data));
		BufferedImage scaledInstance = ImageHelper.getScaledInstance(originalInstance,
				width, RenderingHints.VALUE_INTERPOLATION_BICUBIC, true);
		byte[] result = getImageBytes(scaledInstance, contentType);
		originalInstance.flush();
		scaledInstance.flush();
		return result;
	}

	/**
	 * Convenience method that returns a scaled instance of the
	 * provided {@code BufferedImage}.
	 *
	 * @param img the original image to be scaled
	 * @param targetWidth the desired width of the scaled instance,
	 *    in pixels
	 * @param hint one of the rendering hints that corresponds to
	 *    {@code RenderingHints.KEY_INTERPOLATION} (e.g.
	 *    {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
	 *    {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
	 *    {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
	 * @param higherQuality if true, this method will use a multi-step
	 *    scaling technique that provides higher quality than the usual
	 *    one-step technique (only useful in downscaling cases, where
	 *    {@code targetWidth} or {@code targetHeight} is
	 *    smaller than the original dimensions, and generally only when
	 *    the {@code BILINEAR} hint is specified)
	 * @return a scaled version of the original {@code BufferedImage}
	 */
	public static BufferedImage getScaledInstance(BufferedImage img,
	                                       int targetWidth,
	                                       Object hint,
	                                       boolean higherQuality)
	{
		int targetHeight = (int) ((float) targetWidth / img.getWidth() * img.getHeight());
		int type = (img.getTransparency() == Transparency.OPAQUE) ?
				BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
		BufferedImage ret = img;
		int w, h;
		if (higherQuality) {
			// Use multi-step technique: start with original size, then
			// scale down in multiple passes with drawImage()
			// until the target size is reached
			w = img.getWidth();
			h = img.getHeight();
		} else {
			// Use one-step technique: scale directly from original
			// size to target size with a single drawImage() call
			w = targetWidth;
			h = targetHeight;
		}

		do {
			if (higherQuality && w > targetWidth) {
				w /= 2;
				if (w < targetWidth) {
					w = targetWidth;
				}
			}

			if (higherQuality && h > targetHeight) {
				h /= 2;
				if (h < targetHeight) {
					h = targetHeight;
				}
			}

			BufferedImage tmp = new BufferedImage(w, h, type);
			Graphics2D g2 = tmp.createGraphics();
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
			g2.drawImage(ret, 0, 0, w, h, null);
			g2.dispose();

			ret = tmp;
		} while (w != targetWidth || h != targetHeight);

		return ret;
	}

}
