package milo.utils.media;

import milo.utils.image.Image;
import milo.utils.image.ImageHelper;
import milo.utils.jpa.EntityService;

import javax.imageio.ImageIO;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.ForbiddenException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;


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

	public boolean persistImgToFileSystem(String filePath, String extension, byte[] data) {
		try {
			LOG.info("persistImgToFileSystem " + filePath);
			String completePath = filePath + "/" + extension;
			Files.createDirectories(Paths.get(completePath).getParent());
			File file = new File(completePath);
			ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
			ImageIO.write(ImageIO.read(byteArrayInputStream), extension, file);
			return true;
		} catch(Exception e) {
			LOG.log(Level.WARNING, "caught persistImgToFileSystem " + e.getMessage(), e);
			return false;
		}
	}

	public File retrieveImgFromFileSystem(String filePath) {
		try (Stream<Path> stream = Files.walk(Paths.get(filePath))) {
			Path path = stream.filter(Files::isRegularFile).findFirst().orElse(null);
			boolean isNull = path == null;
			LOG.info("retrieveImgFromFileSystem " + (!isNull) + ": " + filePath);
			return isNull ? null : path.toFile();
		} catch (NoSuchFileException e) {
			LOG.info("retrieveImgFromFileSystem not found " + filePath);
		} catch(Exception e) {
			LOG.log(Level.WARNING, "caught retrieveImgFromFileSystem " + e.getMessage(), e);
		}
		return null;
	}

}





