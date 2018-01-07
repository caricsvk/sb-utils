/* 
 * The MIT License
 *
 * Copyright 2014 Richard Casar <caricsvk@gmail.com>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package milo.utils.jpa;

import milo.utils.jpa.search.EntityFilter;
import milo.utils.jpa.search.OrderType;
import milo.utils.jpa.search.TableSearchQuery;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.SingularAttribute;
import javax.transaction.Transactional;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public abstract class EntityService<E, ID> {

	protected final Class<E> entityClass;
	protected final Class<ID> idClass;

	protected abstract EntityManager getEntityManager();

	public EntityService(Class<E> entityClass, Class<ID> idClass) {
		this.entityClass = entityClass;
		this.idClass = idClass;
	}

	@Transactional
	public E persist(E entity) {
		getEntityManager().persist(entity);
		return entity;
	}

	@Transactional
	public E merge(E entity) {
		return getEntityManager().merge(entity);
	}

	public E find(ID id) {
		return getEntityManager().find(entityClass, id);
	}

	@Transactional
	public void remove(ID id) {
		getEntityManager().remove(getEntityManager().find(entityClass, id));
	}

	public NumericValue count(TableSearchQuery tableSearchQuery) {
		return new NumericValue((long) searchCount(tableSearchQuery));
	}

	public NumericValue sum(String fieldName, TableSearchQuery tableSearchQuery) {
		Field sumField = SearchFieldsPrototype.getSearchFields(entityClass).getField(fieldName);
		return new NumericValue(searchSum(sumField, tableSearchQuery));
	}

	public List<E> search(TableSearchQuery tableSearchQuery) {
		return searchTableQuery(tableSearchQuery);
	}

	private List<E> searchTableQuery(TableSearchQuery tb) {
		CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
		CriteriaQuery<E> cq = cb.createQuery(entityClass);
		Root<E> root = cq.from(entityClass);

		cq.select(root).distinct(true); // vs DISTINCT_ROOT_ENTITY?

		String[] orderPathArray = tb.getOrder().split("\\.");
		Path<Object> orderPath = getPath(root, orderPathArray);
		if (orderPath != null) {
			cq.orderBy(OrderType.DESC.equals(tb.getOrderType()) ? cb.desc(orderPath) : cb.asc(orderPath));
		}

		Query query = tb.createQuery(getEntityManager(), cb, cq, root);
		if (query == null) {
			query = createCommonQuery(cb, cq, root, tb);
		}

		if (tb.getOffset() > 0) {
			query.setFirstResult(tb.getOffset());
		}
		if (tb.getLimit() > 0) {
			query.setMaxResults(tb.getLimit());
		}

		return query.getResultList();
	}

	private Path<Object> getPath(Root<E> root, String[] pathArray) {
		try {
			Path path = root.get(pathArray[0]);
			// oneToMany or manyToMany
			if (root.getModel().getPluralAttributes().stream().filter(
					pluralAttribute -> pluralAttribute.getName().equals(pathArray[0])).count() > 0) {
				path = root.join(pathArray[0]);
			}
			for (int i = 1; i < pathArray.length; i++) {
				path = path.get(pathArray[i]);
			}
			return path;
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}

	private int searchCount(TableSearchQuery tb) {
		CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
		CriteriaQuery cq = cb.createQuery();
		Root<E> root = cq.from(entityClass);
		cq.select(cb.countDistinct(root));
		Query query = tb.createQuery(getEntityManager(), cb, cq, root);
		if (query == null) {
			query = createCommonQuery(cb, cq, root, tb);
		}
		try {
			return ((Long) query.getSingleResult()).intValue();
		} catch (NoResultException ex) {
			return 0;
		}
	}

	private Long searchSum(Field field, TableSearchQuery tb) {
		CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
		CriteriaQuery cq = cb.createQuery();
		Root<E> root = cq.from(entityClass);
		cq.select(cb.sum(root.get(field.getName())));
		Query query = tb.createQuery(getEntityManager(), cb, cq, root);
		if (query == null) {
			query = createCommonQuery(cb, cq, root, tb);
		}
		try {
			return (Long) query.getSingleResult();
		} catch (NoResultException ex) {
			return 0L;
		}
	}

	private SingularAttribute<? super E, ?> getIdAttribute() {
		Metamodel m = getEntityManager().getMetamodel();
		IdentifiableType<E> of = (IdentifiableType<E>) m.managedType(entityClass);
		return of.getId(of.getIdType().getJavaType());
	}

	private Query createCommonQuery(CriteriaBuilder cb, CriteriaQuery cq, Root root, TableSearchQuery tb) {

		List<Predicate> orPredicates = new ArrayList<>();
		List<Predicate> andPredicates = new ArrayList<>();
		List<String> processedFields = new ArrayList<>(tb.getFilterParameters().size());
		SearchFieldsPrototype.SearchFields searchFields = SearchFieldsPrototype.getSearchFields(entityClass);

		// create predicates for concrete filter fields
		for (String key : tb.getFilterParameters().keySet()) {
			EntityFilter entityFilter = tb.getFilterParameters().get(key);
			andPredicates.addAll(createPredicates(entityFilter, searchFields, cb, root));
			processedFields.add(key);
		}

		// create predicates for the rest of allowed fields with 'filter'
		// fulltext search param
		if (tb.getFilter() != null && !tb.getFilter().isEmpty()) {
			for (Field field : searchFields.getAllowedFields()) {
				// jump over fields filtered by concrete filter
				if (!processedFields.contains(field.getName())) {
					orPredicates.add(cb.like(cb.lower(cb.concat(root.<String> get(field.getName()), "::text")),
							"%" + tb.getFilter().toLowerCase() + "%"));
				}
			}
		}

		// join predicates
		if (orPredicates.size() > 0 && andPredicates.size() > 0) {
			cq.where(
					cb.or(
							cb.or(orPredicates.toArray(new Predicate[0])),
							cb.and(andPredicates.toArray(new Predicate[0]))
					)
			);
		} else if (orPredicates.size() > 0) {
			cq.where(cb.or(orPredicates.toArray(new Predicate[0])));
		} else if (andPredicates.size() > 0) {
			cq.where(cb.and(andPredicates.toArray(new Predicate[0])));
		}

		return getEntityManager().createQuery(cq);
	}

//	List<Predicate> orPredicates = new ArrayList<>();
//	for (String value : entityFilter.getValues()) {
//		System.out.println("EntityService.createPredicates -=-=-=-=-=- " + value);
//		orPredicates.add(cb.like(cb.lower(cb.concat(fieldCriteriaPath, "::text")),
//				"%" + value.toLowerCase() + "%"));
//	}
//	predicates.add(cb.or(orPredicates.toArray(new Predicate[orPredicates.size()])));

	private List<Predicate> createPredicates(EntityFilter entityFilter, SearchFieldsPrototype.SearchFields fields,
	                                         CriteriaBuilder cb, Root root) {
		List<Predicate> predicates = new ArrayList<>();
		if (entityFilter == null) {
			return predicates;
		}
		Path path = getPath(root, entityFilter.getFieldName().split("\\."));
		if (path == null) {
			return predicates;
		}
		String[] fieldPath = entityFilter.getFieldName().split("\\.");
		Field field = fields.getField(fieldPath[0]);
//		boolean fieldIsCollection = Collection.class.isAssignableFrom(field.getType())
//				|| List.class.isAssignableFrom(field.getType()) || Set.class.isAssignableFrom(field.getType());
		// TODO why?
//		// when there is not sub-object field and field is collection - only empty check is allowed
//		if (fieldIsCollection && !EntityFilterType.EMPTY.equals(entityFilter.getEntityFilterType())) {
//			return predicates;
//		}
		String firstValue = entityFilter.getFirstValue();
		switch (entityFilter.getEntityFilterType()) {
			case EXACT:
				if (path.getJavaType().equals(boolean.class) || path.getJavaType().equals(Boolean.class)) {
					if (entityFilter.getValue() == null || entityFilter.getValue().isEmpty()
							|| entityFilter.getValue().equals("0") || entityFilter.getValue().equals("false")
							|| entityFilter.getValue().equals("f")) {
						predicates.add(cb.isFalse(path));
					} else {
						predicates.add(cb.isTrue(path));
					}
				} else {
					// TODO try if it works within postgres and with EclipseLink
					// this works with hibernate/mysql
					List<Predicate> orPredicates = entityFilter.getValues().stream().map(value ->
							cb.equal(cb.lower(cb.concat(path, "")), value.toLowerCase())).collect(Collectors.toList());
					predicates.add(cb.or(orPredicates.toArray(new Predicate[orPredicates.size()])));

				}
				break;
			case WILDCARD:
				// tested with hibernate, eclipselink, mysql and postgresql
				List<Predicate> orPredicates = new ArrayList<>();
				for (String value : entityFilter.getValues()) {
					orPredicates.add(cb.like(cb.lower(cb.concat(path, "")), "%" + value.toLowerCase() + "%"));
				}
				predicates.add(cb.or(orPredicates.toArray(new Predicate[orPredicates.size()])));
				break;
			case EMPTY:
				if (Collection.class.isAssignableFrom(path.getJavaType()) || List.class.isAssignableFrom(path.getJavaType())) {
					predicates.add(firstValue.equals("true") ? cb.isEmpty(root.<Collection> get(field.getName()))
							: cb.isNotEmpty(root.<Collection> get(field.getName())));
				} else {
					predicates.add(firstValue.equals("true") ? root.get(field.getName()).isNull()
							: root.get(field.getName()).isNotNull());
				}
				break;
			case MIN_MAX:

				if (BigDecimal.class.isAssignableFrom(path.getJavaType())) {
					BigDecimal valueMin = new BigDecimal(entityFilter.getFirstValue());
					BigDecimal valueMax = new BigDecimal(entityFilter.getSecondValue());
					predicates.add(cb.greaterThanOrEqualTo(path, valueMin));
					predicates.add(cb.lessThanOrEqualTo(path, valueMax));
				} else if (path.getJavaType().isPrimitive() || Number.class.isAssignableFrom(path.getJavaType())) {
					predicates.add(cb.greaterThanOrEqualTo(path, entityFilter.getFirstValue()));
					predicates.add(cb.lessThanOrEqualTo(path, entityFilter.getSecondValue()));
				} else {
					predicates.add(cb.greaterThanOrEqualTo(path, new Timestamp(Long.valueOf(entityFilter.getFirstValue()))));
					predicates.add(cb.lessThanOrEqualTo(path, new Timestamp(Long.valueOf(entityFilter.getSecondValue()))));
				}
				break;
			case MIN:
				if (BigDecimal.class.isAssignableFrom(path.getJavaType())) {
					BigDecimal bigDecimalValue = new BigDecimal(firstValue);
					predicates.add(cb.greaterThanOrEqualTo(path, bigDecimalValue));
				} else if (path.getJavaType().isPrimitive() || Number.class.isAssignableFrom(path.getJavaType())) {
					predicates.add(cb.greaterThanOrEqualTo(path, firstValue));
				} else {
					predicates.add(cb.greaterThanOrEqualTo(path, new Timestamp(Long.valueOf(firstValue))));
				}
				break;
			case MAX:
				if (BigDecimal.class.isAssignableFrom(path.getJavaType())) {
					BigDecimal bigDecimalValue = new BigDecimal(firstValue);
					predicates.add(cb.lessThanOrEqualTo(path, bigDecimalValue));
				} else if (path.getJavaType().isPrimitive() || Number.class.isAssignableFrom(path.getJavaType())) {
					predicates.add(cb.lessThanOrEqualTo(path, firstValue));
				} else {
					predicates.add(cb.lessThanOrEqualTo(path, new Timestamp(Long.valueOf(firstValue))));
				}
				break;
			case EXACT_NOT:
				break;
		}
		return predicates;
	}

	public static class NumericValue {

		private Number value;

		public NumericValue() {
		}

		public NumericValue(Number value) {
			this.value = value;
		}

		public Number getValue() {
			return value;
		}

		public void setValue(Number value) {
			this.value = value;
		}
	}
}
