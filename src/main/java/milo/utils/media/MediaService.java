package milo.utils.media;

import milo.utils.image.Image;
import milo.utils.image.ImageHelper;
import milo.utils.jpa.EntityService;

import javax.persistence.NoResultException;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.ForbiddenException;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;


public abstract class MediaService<T extends AbstractMedia> extends EntityService<T, Long> {

	private static final Logger LOG = Logger.getLogger(MediaService.class.getName());

	public MediaService(Class<T> entityClass) {
		super(entityClass, Long.class);
	}

	protected abstract T createNew();

	@Override
	public T merge(@Valid T entity) {
		throw new ForbiddenException();
	}

	@Override
	public void remove(Long aLong) {
		throw new ForbiddenException();
	}

	@Transactional
	public Long createFromUrl(String url) {
		return createFromUrl(url, 400);
	}

	public Image getFromUrl(String url, int max) {
		Image image = ImageHelper.createFromUrl(url);
		if (image == null) {
			return null;
		}

		if (image.getWidth() > max) {
			BufferedImage scaledInstance = ImageHelper.getScaledInstance(image.getBufferedImage(), max, java.awt.Image.SCALE_AREA_AVERAGING);
			image.setContent(ImageHelper.getImageBytes(scaledInstance, image.getContentType()));
			image.setHeight(scaledInstance.getHeight());
			image.setWidth(scaledInstance.getWidth());
		}
		return image;
	}

	@Transactional
	public Long createFromUrl(String url, int max) {
		try {
			Image image = getFromUrl(url, max);
			if (image != null) {
				return this.create(image, null);
			}
		} catch (Exception ex) {
			LOG.log(Level.WARNING, ex.getMessage(), ex);
		}
		return null;
	}

	@Transactional
	public Long create(Image image, String name) {
		T media = this.createNew();
		media.setName(name != null ? name : UUID.randomUUID().toString() + "-" + image.getName());
		media.setContentType(image.getContentType());
		media.setHeight(image.getHeight());
		media.setWidth(image.getWidth());
		media.setData(image.getContent());
		media.setContentSize(media.getData().length);
		if (media.getName() != null && media.getName().length() < 254 && media.getContentType() != null
				&& media.getContentType().length() < 254 && media.getContentSize() > 0) {
			getEntityManager().persist(media);
			getEntityManager().flush();
		}
		return media.getId();
	}

	@Transactional
	public T create(
			InputStream inputStream,
			String filename,
			String type,
			double maxSideLength,
			String uniqueName
	) {
		try {
			Image image = ImageHelper.create(inputStream, filename, type, null);

			if (image.getWidth() > maxSideLength || image.getHeight() > maxSideLength) {
				int newWidth = (int) (image.getWidth() > maxSideLength ? maxSideLength : image.getWidth() / (image.getHeight() / maxSideLength));
				BufferedImage scaledInstance = ImageHelper.getScaledInstance(image.getBufferedImage(), newWidth,
						java.awt.Image.SCALE_AREA_AVERAGING);
				image.setContent(ImageHelper.getImageBytes(scaledInstance, image.getContentType()));
				image.setHeight(scaledInstance.getHeight());
				image.setWidth(scaledInstance.getWidth());
			}

			T media = createNew();
			media.setName(uniqueName == null ? image.getName() :
					uniqueName + "." + image.getContentType().split("/")[1].split(";")[0]
			);
			media.setContentType(image.getContentType());
			media.setHeight(image.getHeight());
			media.setWidth(image.getWidth());
			media.setData(image.getContent());
			media.setContentSize(media.getData().length);
			if (media.getName() != null && media.getContentType() != null && media.getContentSize() > 0) {
				getEntityManager().persist(media);
				return media;
			}
		} catch (Exception ex) {
			LOG.log(Level.WARNING, ex.getMessage(), ex);
		}
		throw new RuntimeException("Media could not be created, check previous logs for cause.");
	}

	public List<T> findByIds(List<Long> imgIds) {
		if (imgIds == null || imgIds.isEmpty()) {
			return new ArrayList<>();
		}
		return getEntityManager().createNamedQuery(T.FIND_BY_IDS, entityClass).setParameter("ids", imgIds).getResultList();
	}

	public List<T> findResolutionIndexByIds(List<Long> imgIds) {
		if (imgIds == null || imgIds.isEmpty()) {
			return new ArrayList<>();
		}
		return getEntityManager().createNamedQuery(T.FIND_RESOLUTION_INDEX_BY_IDS, entityClass)
				.setParameter("ids", imgIds).getResultList();
	}

	public T findForSerialize() {
		try {
			return getEntityManager().createNamedQuery(T.FIND_FOR_SERIALIZE, entityClass)
					.setMaxResults(1).getSingleResult();
		} catch (NoResultException ex) {
			return null;
		}
	}

	public Long getLastId() {
		return getEntityManager().createNamedQuery(T.GET_LAST_ID, Long.class).getSingleResult();
	}

	public T findByName(String name) {
		try {
			return getEntityManager().createNamedQuery(T.FIND_BY_NAME, entityClass)
					.setParameter("name", name).setMaxResults(1).getSingleResult();
		} catch (NoResultException exception) {
			return null;
		}
	}

//	@Async
//	public void persistToFileIfNotExists(String uniqueName, byte[] data) {
//		// wait 3-15 seconds to persist images to File / prevent resources exhaustion during new images fetch
//		this.scheduler.schedule(() -> {
//			try {
//				if (uniqueName.length() > 255) {
//					LOG.info("persistToFileIfNotExists skips " + uniqueName + " - encoded file name too long");
//					return;
//				}
//				File file = new File(this.imagesDirLocationPath + uniqueName);
//				if (! file.exists()) {
//					LOG.info("creating image " + file.getName());
//					String extension = uniqueName.substring(uniqueName.lastIndexOf(".") + 1);
//					ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
//					ImageIO.write(ImageIO.read(byteArrayInputStream), extension, file );
//				}
//			} catch(Exception e) {
//				LOG.log(Level.WARNING, "caught " + e.getMessage(), e);
//			}
//		}, Instant.now().plusSeconds(3 + new Random().nextInt(12)));
//	}

}





