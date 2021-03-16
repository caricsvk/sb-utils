package milo.utils.documentdb;

import milo.utils.jpa.search.CommonSearchQuery;
import milo.utils.jpa.search.EntityFilter;
import milo.utils.jpa.search.EntityFilterType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DocumentSearchQuery extends CommonSearchQuery {

	private static final Logger LOG = Logger.getLogger(DocumentSearchQuery.class.getName());

	@QueryParam("field")
	@DefaultValue("_all")
	private String field;
	@QueryParam("order")
	@DefaultValue("")
	private String order;
	@QueryParam("id")
	private String id = null;
	private Long scroll;
	private String scrollId;
	private Boolean trackAccurateCount = false;
	private List<String> fields;
	private MultivaluedMap<String, String> queryParameters;
	private QueryBuilder queryBuilder = null;
	private BoolQueryBuilder filterBuilder = null;

	public DocumentSearchQuery() {
	}

	public DocumentSearchQuery(@Context UriInfo ui) {
		List<String> knownKeys = new ArrayList<>();

		knownKeys.add("offset");
		knownKeys.add("limit");
		knownKeys.add("order");
		knownKeys.add("orderType");
		knownKeys.add("filter");
		knownKeys.add("field");

		queryParameters = ui.getQueryParameters();
		List<EntityFilterType> filterTypes = Arrays.asList(EntityFilterType.values());

		for (Map.Entry<String, List<String>> param : queryParameters.entrySet()) {
			if (knownKeys.contains(param.getKey())) {
				continue;
			}

			Optional<EntityFilterType> paramFilterType = filterTypes.stream().filter(entityFilterType ->
					param.getKey().endsWith(entityFilterType.getSuffix())).findFirst();
			if (paramFilterType.isPresent()) {

				if (Arrays.asList(EntityFilterType.MIN, EntityFilterType.MAX).contains(paramFilterType.get())) {
					param.setValue(param.getValue().stream().filter(this::isNumber).collect(Collectors.toList()));
				}

				if (param.getValue().size() == 0) {
					continue;
				}

				String fieldName = param.getKey().replaceFirst(paramFilterType.get().getSuffix(), "");
				EntityFilter entityFilter = new EntityFilter(fieldName, paramFilterType.get(), param.getValue());
				// handle MIN_MAX - catches the second definition - either MIN or MAX and put them together
				if (filterParamExists(fieldName) && EntityFilterType.MIN.equals(entityFilter.getEntityFilterType())) {
					entityFilter.setEntityFilterType(EntityFilterType.MIN_MAX);
					entityFilter.getValues().add(filterParameters.get(fieldName).getValue());
				} else if (filterParamExists(fieldName) && EntityFilterType.MAX.equals(entityFilter.getEntityFilterType())) {
					entityFilter.setEntityFilterType(EntityFilterType.MIN_MAX);
					List<String> existingValues = filterParameters.get(fieldName).getValues();
					existingValues.add(entityFilter.getValue());
					entityFilter.setValues(existingValues);
				}
				filterParameters.put(fieldName, entityFilter);
			}
		}
	}

	private boolean isNumber(String value) {
		try {
			Double.parseDouble(value);
			return true;
		} catch (Exception ex) {
			LOG.log(Level.WARNING, "caught wrong MIN/MAX query: " + ex.getMessage(), ex);
			return false;
		}
	}

	public Long getScroll() {
		return scroll;
	}

	public void setScroll(Long scroll) {
		this.scroll = scroll;
	}

	public String getScrollId() {
		return scrollId;
	}

	public void setFields(List<String> fields) {
		this.fields = fields;
	}
	public void setScrollId(String scrollId) {
		this.scrollId = scrollId;
	}

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}

	@Override
	public String getOrder() {
		return order;
	}

	@Override
	public void setOrder(String order) {
		this.order = order;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public QueryBuilder getQueryBuilder() {
		return queryBuilder;
	}

	public void setQueryBuilder(QueryBuilder queryBuilder) {
		this.queryBuilder = queryBuilder;
	}

	public BoolQueryBuilder getFilterBuilder() {
		return filterBuilder;
	}

	public void setFilterBuilder(BoolQueryBuilder filterBuilder) {
		this.filterBuilder = filterBuilder;
	}

	public MultivaluedMap<String, String> getQueryParameters() {
		return queryParameters;
	}

	public List<String> getFields() {
		if (fields == null) {
			fields = new ArrayList<>();
		}
		return fields;
	}

	public Boolean getTrackAccurateCount() {
		return trackAccurateCount;
	}

	public void setTrackAccurateCount(Boolean trackAccurateCount) {
		this.trackAccurateCount = trackAccurateCount;
	}

	@Override
	public String toString() {
		return "DocumentSearchQuery{" +
				"field='" + field + '\'' +
				", order='" + order + '\'' +
				", id='" + id + '\'' +
				", scroll=" + scroll +
				", scrollId='" + scrollId + '\'' +
				", fields=" + fields +
				", queryParameters=" + queryParameters +
				", queryBuilder=" + queryBuilder +
				", filterBuilder=" + filterBuilder +
				", offset=" + offset +
				", limit=" + limit +
				", order='" + order + '\'' +
				", orderType=" + orderType +
				", filter='" + filter + '\'' +
				", filterParameters=" + filterParameters +
				'}';
	}
}
