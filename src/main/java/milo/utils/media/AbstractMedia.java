package milo.utils.media;

import jakarta.persistence.Basic;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

@MappedSuperclass
@Cacheable(false)
@NamedQueries({
		@NamedQuery(
				name = AbstractMedia.FIND_BY_IDS,
				query = "SELECT entity FROM Media entity WHERE entity.id in :ids"
		),
		@NamedQuery(
				name = AbstractMedia.FIND_BY_NAME,
				query = "SELECT entity FROM Media entity WHERE entity.name = :name"
		),
		@NamedQuery(
				name = AbstractMedia.FIND_RESOLUTION_INDEX_BY_IDS,
				query = "SELECT new Media(entity.id, entity.height, entity.width, entity.serializedTo) FROM Media entity WHERE entity.id in :ids"
		),
		@NamedQuery(
				name = AbstractMedia.FIND_FOR_SERIALIZE,
				query = "SELECT entity FROM Media entity WHERE entity.serializedTo IS NULL ORDER BY entity.id"
		),
		@NamedQuery(
				name = AbstractMedia.GET_LAST_ID,
				query = "SELECT MAX(entity.id) FROM Media entity"
		),
		@NamedQuery(
				name = AbstractMedia.REMOVE_SERIALIZED_DATA,
				query = "UPDATE Media entity SET entity.data = :emptyData, entity.contentSize = 0 WHERE entity.id >= :fromId AND entity.id <= :toId AND entity.contentSize > 0 AND entity.serializedTo IS NOT NULL AND entity.serializedTo <> ''"
		)
})
public abstract class AbstractMedia implements Serializable {
	private static final long serialVersionUID = 1L;
	public static final String FIND_BY_IDS = "Media.findByIds";
	public static final String FIND_BY_NAME = "Media.findByName";
	public static final String FIND_RESOLUTION_INDEX_BY_IDS = "Media.findResolutionsByIds";
	public static final String FIND_FOR_SERIALIZE = "Media.findForSerialize";
	public static final String GET_LAST_ID = "Media.getLastId";
	public static final String REMOVE_SERIALIZED_DATA = "Media.RemoveSerialized";
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private Long originalId;
	private String serializedTo;
	@NotNull
	@Column(nullable = false)
	private String name;
	@NotNull
	@Column(nullable = false)
	private String contentType;
	@Lob
	@NotNull
	@Basic(fetch = FetchType.LAZY)
	@Column(nullable = false)
	private byte[] data;    // bytea NOT NULL,
	private Integer height;
	private Integer width;
	private Integer contentSize;
	@Transient
	private Double resolutionIndex;

	public AbstractMedia() {
	}

	public AbstractMedia(Long id, Integer height, Integer width, String serializedTo) {
		setId(id);
		setHeight(height);
		setWidth(width);
		setSerializedTo(serializedTo);
	}

	public Double getResolutionIndex() {
		resolutionIndex = (width != null & height != null) ? (double) width / height : null;
		return resolutionIndex;
	}

	public void setResolutionIndex(Double resolutionIndex) {
		this.resolutionIndex = resolutionIndex;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		if (name.length() > 254) {
			name = name.substring(name.length() - 250);
		}
		this.name = name.replaceAll(" ", "-");
	}

	public Long getOriginalId() {
		return originalId;
	}

	public void setOriginalId(Long originalId) {
		this.originalId = originalId;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		if (contentType.length() > 254) {
			contentType = contentType.substring(contentType.length() - 250);
		}
		this.contentType = contentType;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public Integer getHeight() {
		return height;
	}

	public void setHeight(Integer height) {
		this.height = height;
	}

	public Integer getWidth() {
		return width;
	}

	public void setWidth(Integer width) {
		this.width = width;
	}

	public Integer getContentSize() {
		return contentSize;
	}

	public void setContentSize(Integer contentSize) {
		this.contentSize = contentSize;
	}

	public String getSerializedTo() {
		return serializedTo;
	}

	public void setSerializedTo(String serializedTo) {
		this.serializedTo = serializedTo;
	}

	public String getNameWithId() {
		return this.name.replaceAll("(.*)(\\.[a-zA-Z]+)$", "$1-" + this.id + "$2");
	}

	@Override
	public String toString() {
		return "Media{" +
				"id=" + id +
				", originalId=" + originalId +
				", serializedTo='" + serializedTo + '\'' +
				", name='" + name + '\'' +
				", contentType='" + contentType + '\'' +
				", height=" + height +
				", width=" + width +
				", contentSize=" + contentSize +
				", resolutionIndex=" + resolutionIndex +
				'}';
	}
}



