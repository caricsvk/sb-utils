package milo.utils.jpa.search;

public enum EntityFilterType {

	WILDCARD("_wild"), MIN_MAX("_minmax"), MIN("_min"), MAX("_max"), EMPTY("_empty"), EXACT("_exact"), EXACT_NOT(
			"_exact_not"); // or null

	private String suffix;

	/**
	 * Get the value of suffix
	 *
	 * @return the value of suffix
	 */
	public String getSuffix() {
		return suffix;
	}

	/**
	 * Set the value of suffix
	 *
	 * @param suffix
	 *            new value of suffix
	 */
	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	private EntityFilterType(String suffix) {
		this.suffix = suffix;
	}

}