package milo.utils.media;

import milo.utils.auth.AuthService;
import milo.utils.auth.AuthUser;
import milo.utils.image.ImageHelper;

import javax.imageio.ImageIO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;


public abstract class MediaResource {

	private static Logger LOG = Logger.getLogger(MediaResource.class.getName());

	protected abstract AuthService<AuthUser> getAuthService();
	protected abstract MediaService<AbstractMedia> getMediaService();
	protected abstract HttpServletRequest getHttpServletRequest();

	@POST
	@Path("image-from-url")
	public String createImageFromUrl(String url) {
		return "{\"mediaId\": " + getMediaService().createFromUrl(url) + "}";
	}

	@GET
	@Path("image-from-url")
	public void getImageFromUrl(
			@QueryParam("url") String url,
			@QueryParam("max-width") @DefaultValue("1200") Integer maxWidth,
			@Context Request request,
			@Suspended final AsyncResponse asyncResponse
	) {
		milo.utils.image.Image image = getMediaService().getFromUrl(url, maxWidth);
		AbstractMedia media = this.getMediaService().createNew();
		media.setName(Arrays.toString(Base64.getEncoder().encode(url.getBytes(StandardCharsets.UTF_8))));
		media.setContentType(image.getContentType());
		media.setHeight(image.getHeight());
		media.setWidth(image.getWidth());
		media.setData(image.getContent());
		media.setContentSize(media.getData().length);
		respondWithImage(media, asyncResponse, request);
	}

	public AbstractMedia uploadFile(
			Integer maxWidth,
			InputStream inputStream,
			String filename,
			String type
	) {
		HttpSession session = getHttpServletRequest().getSession(false);
		AuthUser loggedUser = getAuthService().getLoggedUser(session);
		LOG.info("uploading file by user " + loggedUser);
		String randomUuid = UUID.randomUUID().toString().substring(0, 8);
		return this.getMediaService().create(
				inputStream, filename, type, maxWidth, randomUuid + "-" + loggedUser.getId()
		);
	}

	@GET
	@Path("/")
	public void getContentByUrl(
			@QueryParam("name") String name,
			@Context Request request,
			@Suspended final AsyncResponse asyncResponse
	) {
		respondWithImage(getMediaService().findByName(name), asyncResponse, request);
	}

	@GET
	@Path("{nameEncoded}")
	public void getContentByName(
			@PathParam("nameEncoded") String name,
			@Context Request request,
			@Suspended final AsyncResponse asyncResponse
	) throws ExecutionException {
		this.getContentByUrl(name, request, asyncResponse);
	}

	@GET
	@Path("{id}/content")
	public Response getContent(@PathParam("id") Long id, @QueryParam("type") Integer type, @QueryParam("size") Integer size) {
		try {
			AbstractMedia media = getMediaService().find(id);
			byte[] data = media.getData();
			if (type != null) {
				long time = System.currentTimeMillis();
				BufferedImage scaledInstance = imageQualityComparator(type, size, data);
				data = ImageHelper.getImageBytes(scaledInstance, media.getContentType());
				System.out.println("MediaResource.getContentById. Type: " + type + " 10 images scaling took " + (System.currentTimeMillis() - time)
						+ " ms, size: " + data.length);
			}
			return Response.ok(data, media.getContentType()).build();
		} catch (Exception ex) {
			Logger.getAnonymousLogger().log(Level.INFO, ex.getMessage(), ex);
			return Response.noContent().build();
		}
	}

	@GET
	@Path("{id}/meta")
	public AbstractMedia getMetadata(@PathParam("id") Long id) {
		AbstractMedia media = getMediaService().find(id);
		media.setData(null);
		return media;
	}

	public static void respondWithImage(AbstractMedia media, AsyncResponse asyncResponse, Request request) {
		asyncResponse.resume(media == null ? Response.noContent().build() : getImageResponse(
				media.hashCode(), media.getData(), media.getContentType(), 31536000, request
		));
	}

	public static Response getImageResponse(
			int hash, byte[] data, String contentType, int maxAgeSeconds, Request request
	) {
		CacheControl cacheControl = new CacheControl();
		cacheControl.setMaxAge(maxAgeSeconds);
		EntityTag etag = new EntityTag(Integer.toString(hash));
		Response.ResponseBuilder builder = request.evaluatePreconditions(etag);
		if (builder == null) {
			builder = Response.ok(data, contentType).tag(etag);
		}
		return builder.cacheControl(cacheControl).build();
	}

	private BufferedImage imageQualityComparator(int type, Integer size, byte[] data) throws IOException {
		if (size == null) {
			size = 160;
		}
		BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(data));
		BufferedImage scaledInstance = null;
		if (type == 1) {
			for (int i = 0; i < 10; i ++) {
				scaledInstance = ImageHelper.getScaledInstance(bufferedImage, size, Image.SCALE_AREA_AVERAGING);
			}
			return scaledInstance;
		} else if (type == 2) {
			for (int i = 0; i < 10; i ++) {
				scaledInstance = ImageHelper.getScaledInstance(bufferedImage, size, Image.SCALE_SMOOTH);
			}
			return scaledInstance;
		} else if (type == 3) {
			for (int i = 0; i < 10; i ++) {
				scaledInstance = ImageHelper.getScaledInstance(bufferedImage, size, Image.SCALE_REPLICATE);
			}
			return scaledInstance;
		} else if (type == 4) {
			for (int i = 0; i < 10; i ++) {
				scaledInstance = ImageHelper.getScaledInstance(bufferedImage, size, RenderingHints.VALUE_INTERPOLATION_BICUBIC, true);
			}
			return scaledInstance;
		} else if (type == 5) {
			for (int i = 0; i < 10; i ++) {
				scaledInstance = ImageHelper.getScaledInstance(bufferedImage, size, RenderingHints.VALUE_INTERPOLATION_BILINEAR, true);
			}
			return scaledInstance;
		} else if (type == 6) {
			for (int i = 0; i < 10; i ++) {
				scaledInstance = ImageHelper.getScaledInstance(bufferedImage, size, RenderingHints.VALUE_INTERPOLATION_BICUBIC, false);
			}
			return scaledInstance;
		}
		return bufferedImage;
	}

}



