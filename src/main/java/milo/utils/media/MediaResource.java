package milo.utils.media;

import milo.utils.auth.AuthService;
import milo.utils.auth.AuthUser;
import milo.utils.image.ImageHelper;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiConsumer;
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
		long methodStart = System.currentTimeMillis();
		AbstractMedia media = getMediaService().findByName(name);
		long findTook = System.currentTimeMillis() - methodStart;
		CacheControl cacheControl = new CacheControl();
		cacheControl.setMaxAge(31536000);

		BiConsumer<Object, Long> resume = (Object response, Long downloadTime) -> {
			asyncResponse.resume(response);
			LOG.info("getContentByUrl took " + (System.currentTimeMillis() - methodStart) +
					"ms, find took " + findTook +
					(downloadTime == null ? "" : ", download took " + downloadTime) + ", " + name);
		};

		if (media != null) {
			EntityTag etag = new EntityTag(Integer.toString(media.hashCode()));
			Response.ResponseBuilder builder = request.evaluatePreconditions(etag);
			if (builder == null) {
				builder = Response.ok(media.getData(), media.getContentType()).tag(etag);
			}
			resume.accept(builder.cacheControl(cacheControl).build(), null);
//			getMediaService().persistToFileIfNotExists(media.getName(), media.getData());
		} else {
			resume.accept(Response.noContent().build(), null);
		}
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



