package milo.utils.jpa.search;

import javax.validation.constraints.Max;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CommonSearchQuery {

	@QueryParam("offset")
	@DefaultValue("0")
	protected Integer offset = 0;
	@QueryParam("limit")
	@DefaultValue("0")
	@Max(100)
	protected Integer limit = 10;
	@QueryParam("order")
	@DefaultValue("id")
	protected String order = "id";
	@QueryParam("orderType")
	@DefaultValue("DESC")
	protected OrderType orderType = OrderType.ASC;
	@QueryParam("filter")
	@DefaultValue("")
	protected String filter = "";

	protected Map<String, EntityFilter> filterParameters = new HashMap<>();

	protected boolean filterParamExists(String name) {
		return filterParameters.containsKey(name);
	}

	public Map<String, EntityFilter> getFilterParameters() {
		return filterParameters;
	}

	public void setFilterParameters(Map<String, EntityFilter> filterParameters) {
		this.filterParameters = filterParameters;
	}

	public void setFilterParam(String key, EntityFilterType type, String... values) {
		this.getFilterParameters().put(key, new EntityFilter(key, type, Arrays.asList(values)));
	}

	public void putEntityFilter(EntityFilter entityFilter) {
		getFilterParameters().put(entityFilter.getFieldName(), entityFilter);
	}

	public Integer getOffset() {
		return offset;
	}

	public void setOffset(Integer offset) {
		this.offset = offset;
	}

	public Integer getLimit() {
		return limit;
	}

	public void setLimit(Integer limit) {
		this.limit = limit;
	}

	public String getOrder() {
		return order;
	}

	public void setOrder(String order) {
		this.order = order;
	}

	public OrderType getOrderType() {
		return orderType;
	}

	public void setOrderType(OrderType orderType) {
		this.orderType = orderType;
	}

	public String getFilter() {
		return filter;
	}

	public void setFilter(String search) {
		this.filter = search;
	}

}
