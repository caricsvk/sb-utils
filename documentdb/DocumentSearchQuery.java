package milo.utils.documentdb;

import milo.utils.jpa.search.CommonSearchQuery;
import milo.utils.jpa.search.EntityFilter;
import milo.utils.jpa.search.EntityFilterType;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentSearchQuery extends CommonSearchQuery {

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

	Map<String, EntityFilter> filterParameters = new HashMap<>();

	private QueryBuilder queryBuilder = null;
	private FilterBuilder filterBuilder = null;

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

		MultivaluedMap<String, String> queryParameters = ui.getQueryParameters();
		EntityFilterType[] filterTypes = EntityFilterType.values();

		for (Map.Entry<String, List<String>> param : queryParameters.entrySet()) {
			if (knownKeys.contains(param.getKey())) {
				continue;
			}
			boolean filterMatch = false;
			for (EntityFilterType entityFilterType : filterTypes) {
				if (param.getKey().endsWith(entityFilterType.getSuffix())
						&& !param.getValue().get(0).isEmpty()) {
					String fieldName = param.getKey().replaceFirst(entityFilterType.getSuffix(), "");
					EntityFilter entityFilter = new EntityFilter(fieldName, entityFilterType, param.getValue());
					//handle MIN_MAX
					if (filterParameters.get(fieldName) != null
							&& EntityFilterType.MIN.equals(entityFilter.getEntityFilterType())) {

						entityFilter.setEntityFilterType(EntityFilterType.MIN_MAX);
						entityFilter.getValues().add(filterParameters.get(fieldName).getValue());
					} else if (filterParameters.get(fieldName) != null
							&& EntityFilterType.MAX.equals(entityFilter.getEntityFilterType())) {

						entityFilter.setEntityFilterType(EntityFilterType.MIN_MAX);
						filterParameters.get(fieldName).getValues().add(entityFilter.getValue());
						entityFilter.setValues(filterParameters.get(fieldName).getValues());
					}
					filterParameters.put(fieldName, entityFilter);
					filterMatch = true;
					break;
				}
			}
			if (!filterMatch) {
				EntityFilter entityFilter = new EntityFilter(param.getKey(), EntityFilterType.EXACT, param.getValue());
				filterParameters.put(param.getKey(), entityFilter);
			}
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

	public FilterBuilder getFilterBuilder() {
		return filterBuilder;
	}

	public void setFilterBuilder(FilterBuilder filterBuilder) {
		this.filterBuilder = filterBuilder;
	}

	public Map<String, EntityFilter> getFilterParameters() {
		return filterParameters;
	}

	public void setFilterParameters(Map<String, EntityFilter> filterParameters) {
		this.filterParameters = filterParameters;
	}

	@Override
	public String toString() {
		return "DocumentSearchQuery{" +
				"field='" + field + '\'' +
				", order='" + order + '\'' +
				", id='" + id + '\'' +
				", scroll=" + scroll +
				", scrollId='" + scrollId + '\'' +
				", filterParameters=" + filterParameters +
				", queryBuilder=" + queryBuilder +
				", filterBuilder=" + filterBuilder +
				", filter=" + getFilter() +
				'}';
	}
}
