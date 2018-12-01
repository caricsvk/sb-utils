package milo.utils.jpa.search;

import javax.validation.constraints.Max;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

public class CommonSearchQuery {

	@QueryParam("offset")
	@DefaultValue("0")
	private Integer offset = 0;
	@QueryParam("limit")
	@DefaultValue("0")
	@Max(100)
	private Integer limit = 0;
	@QueryParam("order")
	@DefaultValue("id")
	private String order = "id";
	@QueryParam("orderType")
	@DefaultValue("DESC")
	private OrderType orderType = OrderType.ASC;
	@QueryParam("filter")
	@DefaultValue("")
	private String filter = "";

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
