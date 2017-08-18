package milo.utils.jpa.search;

import java.util.List;

public class EntityFilter {

	private EntityFilterType entityFilterType = EntityFilterType.WILDCARD;
	private List<String> values;
	private String fieldName;

	public EntityFilter() {
	}

	public EntityFilter(String fieldName, EntityFilterType entityFilterType, List<String> values) {
		this.fieldName = fieldName;
		this.entityFilterType = entityFilterType;
		this.values = values;
	}

	public EntityFilterType getEntityFilterType() {
		return entityFilterType;
	}

	public void setEntityFilterType(EntityFilterType entityFilterType) {
		this.entityFilterType = entityFilterType;
	}

	public List<String> getValues() {
		return values;
	}

	public void setValues(List<String> values) {
		this.values = values;
	}

	public String getValue() {
		return getFirstValue();
	}

	public String getFirstValue() {
		return this.values == null || this.values.isEmpty() ? null : this.values.get(0);
	}

	public String getSecondValue() {
		return this.values == null || this.values.size() < 2 ? null : this.values.get(1);
	}

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	@Override
	public String toString() {
		return "EntityFilter{" + "entityFilterType=" + entityFilterType + ", values=" + values + ", fieldName='"
				+ fieldName + '\'' + '}';
	}
}