package milo.utils.jpa.search;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableSearchQuery extends CommonSearchQuery {

	private Map<String, EntityFilter> filterParameters = new HashMap<>();
	private String[] safeOrderPath = null;

	public static TableSearchQuery createEmptyInstance() {
		return new TableSearchQuery(null);
	}

	public TableSearchQuery(@Context UriInfo ui) {

		if (ui == null) {
			return;
		}

		MultivaluedMap<String, String> queryParameters = ui.getQueryParameters();
		EntityFilterType[] filterTypes = EntityFilterType.values();

		for (Map.Entry<String, List<String>> param : queryParameters.entrySet()) {
			boolean filterMatch = false;
			for (EntityFilterType entityFilterType : filterTypes) {
				if (param.getKey().endsWith(entityFilterType.getSuffix()) && !param.getValue().get(0).isEmpty()) {

					String fieldName = param.getKey().replaceFirst(entityFilterType.getSuffix(), "");
					EntityFilter entityFilter = new EntityFilter(fieldName, entityFilterType, param.getValue());
					// handle MIN_MAX
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
			// if (!filterMatch) {
			// EntityFilter entityFilter = new EntityFilter(param.getKey(),
			// EntityFilterType.EXACT, param.getValue());
			// filterParameters.put(param.getKey(), entityFilter);
			// }
		}
	}

	public Query createQuery(EntityManager em, CriteriaBuilder cb, CriteriaQuery cq, Root root) {
		return null;
	}

	public Map<String, EntityFilter> getFilterParameters() {
		return filterParameters;
	}

	public void setFilterParameters(Map<String, EntityFilter> filterParameters) {
		this.filterParameters = filterParameters;
	}

	public void putEntityFilter(EntityFilter entityFilter) {
		getFilterParameters().put(entityFilter.getFieldName(), entityFilter);
	}

	public String[] getSafeOrderPath() {
		return safeOrderPath;
	}

	public void setSafeOrderPath(String[] safeOrderPath) {
		this.safeOrderPath = safeOrderPath;
	}
}
