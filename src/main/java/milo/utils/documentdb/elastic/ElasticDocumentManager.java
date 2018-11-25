package milo.utils.documentdb.elastic;

import milo.utils.documentdb.DocumentEntity;
import milo.utils.documentdb.DocumentJsonSerializer;
import milo.utils.documentdb.DocumentManager;
import milo.utils.documentdb.DocumentSearchQuery;
import milo.utils.jpa.search.EntityFilter;
import milo.utils.jpa.search.EntityFilterType;
import milo.utils.jpa.search.OrderType;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.lang.annotation.AnnotationFormatError;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class ElasticDocumentManager implements DocumentManager {

	private static final String ALL = "_all";
	private Client client;

	protected abstract String getElasticCluster();

	protected abstract String getElasticHost();

	protected abstract int getElasticPort();

	protected abstract String getLocaleRegion();

	protected abstract DocumentJsonSerializer getJsonSerializer();

	@PostConstruct
	public void init() {
		Settings s = ImmutableSettings.settingsBuilder().put("cluster.name", getElasticCluster()).build();
		client = new TransportClient(s).addTransportAddress(new InetSocketTransportAddress(getElasticHost(), getElasticPort()));
	}

	@PreDestroy
	public void destroy() {
		client.close();
	}

	private ElasticIndexType getIndexType(Class<?> documentClass, String market) {
		if (documentClass != null && documentClass.isAnnotationPresent(ElasticDocument.class)) {

			String elasticIndexPrefix = getLocaleRegion() == null ? "local" : getLocaleRegion().toLowerCase();
			if (market != null && market.length() > 0) {
				elasticIndexPrefix = market;
			}

			ElasticDocument annotation = documentClass.getAnnotation(ElasticDocument.class);
			ElasticIndexType elasticIndexType = new ElasticIndexType();

			String index = annotation.index();
			if (index.contains("%D") || index.contains("%M") || index.contains("%Y")) {
				Calendar c = Calendar.getInstance();
				int month = c.get(Calendar.MONTH) + 1;
				int year = c.get(Calendar.YEAR);
				int day = c.get(Calendar.DAY_OF_MONTH);
				String monthString = String.valueOf(month);
				String dayString = String.valueOf(day);
				if (monthString.length() == 1) {
					monthString = "0" + monthString;
				}
				if (dayString.length() == 1) {
					dayString = "0" + dayString;
				}
				index = index.replaceAll("%D", dayString);
				index = index.replaceAll("%M", monthString);
				index = index.replaceAll("%Y", String.valueOf(year));
			}

			elasticIndexType.setIndex(elasticIndexPrefix + "_" + index);
			elasticIndexType.setType(annotation.type());
			return elasticIndexType;
		} else {
			throw new AnnotationFormatError("Provided entity is not annotated with ElasticDocument annotation.");
		}
	}

	private ElasticIndexType getIndexType(Class<?> documentClass) {
		return getIndexType(documentClass, null);
	}

	@Override
	public String insert(Object document) {
		try {
			ElasticIndexType elasticIndexType = getIndexType(document.getClass());
			String json = getJsonSerializer().serialize(document);
			IndexResponse response = client.prepareIndex(elasticIndexType.getIndex(), elasticIndexType.getType()).setRefresh(true).setSource(json).execute().actionGet();
			return response.getId();
		} catch (IOException ex) {
			Logger.getLogger(ElasticDocumentManager.class.getName()).log(Level.SEVERE, null, ex);
			return null;
		}
	}

	@Override
	public void update(DocumentEntity document) {
		update(document, null);
	}

	@Override
	public void update(DocumentEntity document, String market) {
		try {
			ElasticIndexType elasticIndexType = getIndexType(document.getClass(), market);
			String json = getJsonSerializer().serialize(document);
			UpdateRequest updateRequest = new UpdateRequest();
			updateRequest.index(elasticIndexType.getIndex());
			updateRequest.type(elasticIndexType.getType());
			updateRequest.id(document.getId());
			updateRequest.doc(json);
			client.update(updateRequest).get();
		} catch (IOException | InterruptedException | ExecutionException ex) {
			Logger.getLogger(ElasticDocumentManager.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	@Override
	public <T extends DocumentEntity> T get(Class<T> documentClass, Object id) {

		return getFromIndex(documentClass, id, null);
	}

	@Override
	public <T extends DocumentEntity> T getFromIndex(Class<T> documentClass, Object id, String index) {

		ElasticIndexType elasticIndexType = getIndexType(documentClass);
		if (index != null) {
			elasticIndexType.setIndex(index);
		}
		GetResponse response = getResponse(id, elasticIndexType);

		String json = response.getSourceAsString();
		T object = null;

		try {
			object = getJsonSerializer().deserialize(json, documentClass);
			object.setId(response.getId());
		} catch (IOException ex) {
			Logger.getLogger(ElasticDocumentManager.class.getName()).log(Level.SEVERE, null, ex);
		}
		return object;

	}

	private GetResponse getResponse(Object id, ElasticIndexType elasticIndexType) {
		return client.prepareGet(elasticIndexType.getIndex(), elasticIndexType.getType(), (String) id)
				.execute()
				.actionGet();
	}

	@Override
	public <T extends DocumentEntity> List<T> find(Class<T> documentClass, DocumentSearchQuery dsr) {
		return findInIndex(documentClass, dsr, null);
	}

	@Override
	public <T extends DocumentEntity> List<T> findInIndex(Class<T> documentClass, DocumentSearchQuery dsr, String index) {

		ElasticIndexType elasticIndexType = getIndexType(documentClass);
		if (index != null) {
			elasticIndexType.setIndex(index);
		}
		SearchResponse searchResponse = search(elasticIndexType, dsr);
		List<T> objects = new ArrayList<>();
		for (SearchHit sh : searchResponse.getHits()) {
			try {
				T object = getJsonSerializer().deserialize(sh.getSourceAsString(), documentClass);
				object.setId(sh.getId());
				objects.add(object);
			} catch (IOException ex) {
				Logger.getLogger(ElasticDocumentManager.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		return objects;
	}

	@Override
	public Long findCount(Class<?> documentClass, DocumentSearchQuery dsr) {
		dsr.setLimit(0);
		return search(getIndexType(documentClass), dsr).getHits().getTotalHits();
	}

	private SearchResponse search(ElasticIndexType elasticIndexType, DocumentSearchQuery dsr) {

		QueryBuilder qb = null;
		FilterBuilder fb = null;

		if (dsr.getFilterBuilder() != null || dsr.getQueryBuilder() != null) {
			fb = dsr.getFilterBuilder();
			qb = dsr.getQueryBuilder();
		} else if (dsr.getId() != null && !dsr.getId().isEmpty()) {
//				dsr.getFilter().replaceAll("-", "\\-")
			qb = QueryBuilders.matchQuery("_id", dsr.getId());
		} else if (dsr.getFilter() != null && !dsr.getFilter().isEmpty()) {
			qb = QueryBuilders.matchQuery("_all", dsr.getFilter());
		} else {
			AndFilterBuilder andFilterBuilder = FilterBuilders.andFilter();
			for (Map.Entry<String, EntityFilter> entry : dsr.getFilterParameters().entrySet()) {
				if (EntityFilterType.WILDCARD.equals(entry.getValue().getEntityFilterType())){
					qb = QueryBuilders.matchQuery(entry.getValue().getFieldName(),
							entry.getValue().getValue().toLowerCase());
				} else {
					andFilterBuilder.add(createPredicates(entry.getValue()));
				}
			}
			fb = andFilterBuilder;
		}

		if (dsr.getScrollId() != null) {
			SearchResponse searchResponse = client.prepareSearchScroll(dsr.getScrollId())
					.setScroll(new TimeValue(dsr.getScroll())).execute().actionGet();
			dsr.setScrollId(searchResponse.getScrollId());
			return searchResponse;
		} else {
			SearchRequestBuilder srb = client.prepareSearch(elasticIndexType.getIndex());

			srb.setVersion(true);
			srb.setTypes(elasticIndexType.getType());

			if (fb != null) {
				srb.setPostFilter(fb);
			}
			if (qb != null) {
				srb.setQuery(qb);
			}
			if (dsr.getLimit() != null && dsr.getLimit() > 0) {
				srb.setSize(dsr.getLimit());
			}
			if (dsr.getOffset() != null && dsr.getOffset() > 0) {
				srb.setFrom(dsr.getOffset());
			}
			if (dsr.getOrder() != null && !(dsr.getOrder().isEmpty())) {
				FieldSortBuilder fsb = SortBuilders.fieldSort(dsr.getOrder());
				if (dsr.getOrderType().equals(OrderType.ASC)) {
					fsb.order(SortOrder.ASC);
				} else {
					fsb.order(SortOrder.DESC);
				}
				srb.addSort(fsb);
			}
			if (dsr.getScroll() != null && dsr.getScroll() > 0) {
				srb.setScroll(new TimeValue(dsr.getScroll()));
				srb.setSearchType(SearchType.SCAN);
			}
//			System.out.println("ElasticDocumentManager.search: " + srb.toString());
			SearchResponse searchResponse = srb.execute().actionGet();
			dsr.setScrollId(searchResponse.getScrollId()); //in case of scrolling

			return searchResponse;
		}
	}

	private FilterBuilder createPredicates(EntityFilter entityFilter) {
		OrFilterBuilder orFilterBuilder = FilterBuilders.orFilter();
		AndFilterBuilder andFilterBuilder = FilterBuilders.andFilter();
		if (entityFilter == null) {
			return orFilterBuilder;
		}
		switch (entityFilter.getEntityFilterType()) {
			case EXACT_NOT:
				for (String value : entityFilter.getValues()) {
					TermFilterBuilder termFilterBuilder = FilterBuilders.termFilter(
							entityFilter.getFieldName(), value);
					if (entityFilter.getValues().size() == 1) {
						return termFilterBuilder;
					}
					orFilterBuilder.add(FilterBuilders.notFilter(termFilterBuilder));
				}
				break;
			case EXACT:
				for (String value : entityFilter.getValues()) {
					TermFilterBuilder termFilterBuilder = FilterBuilders.termFilter(
							entityFilter.getFieldName(), value);
					if (entityFilter.getValues().size() == 1) {
						return termFilterBuilder;
					}
					orFilterBuilder.add(termFilterBuilder);
				}
				break;
			case EMPTY:
				//TODO
				break;
			case MIN:
				return FilterBuilders.rangeFilter(entityFilter.getFieldName()).from(entityFilter.getFirstValue());
			case MAX:
				return FilterBuilders.rangeFilter(entityFilter.getFieldName()).to(entityFilter.getFirstValue());
			case MIN_MAX:
				return FilterBuilders.rangeFilter(entityFilter.getFieldName())
						.from(entityFilter.getFirstValue())
						.to(entityFilter.getSecondValue());
		}
		return EntityFilterType.EXACT_NOT.equals(entityFilter.getEntityFilterType()) ? andFilterBuilder : orFilterBuilder;
	}

	@Override
	public void delete(Class<?> documentClass, Object id) {
		ElasticIndexType elasticIndexType = getIndexType(documentClass);
		DeleteRequestBuilder requestBuilder = client.prepareDelete(elasticIndexType.getIndex(), elasticIndexType.getType(), (String) id);
		requestBuilder.execute().actionGet();
	}

	public <T extends DocumentEntity> Aggregations aggregations(Class<T> documentClass, AbstractAggregationBuilder aab) {
		return aggregations(documentClass, aab, null);
	}

	public <T extends DocumentEntity> Aggregations aggregations(Class<T> documentClass, AbstractAggregationBuilder aab,
	                                                            QueryBuilder queryBuilder) {
		ElasticIndexType elasticIndexType = getIndexType(documentClass);
		SearchRequestBuilder srb = client.prepareSearch(elasticIndexType.getIndex());
		srb.setSize(0);
		srb.setTypes(elasticIndexType.getType());
		srb.setSearchType(SearchType.COUNT);
		srb.addAggregation(aab);
		srb.setQuery(queryBuilder);
//		System.out.println("ElasticDocumentManager.aggregations ---- " + srb.toString());
		return srb.execute().actionGet().getAggregations();
	}

	public <T extends DocumentEntity> List<Map<String, SearchHitField>> getFields(Class<T> lookInClass,
	                                                                              FilterBuilder filterBuilder,
	                                                                              List<String> fields,
	                                                                              int limit) {
		ElasticIndexType elasticIndexType = getIndexType(lookInClass);
		SearchRequestBuilder srb = client.prepareSearch(elasticIndexType.getIndex());
		srb.setSize(limit);
		srb.setTypes(elasticIndexType.getType());
		srb.setPostFilter(filterBuilder);
		fields.forEach(srb::addField);
		List<Map<String, SearchHitField>> results = new ArrayList<>();
//		System.out.println("ElasticDocumentManager.getFields -------- " + srb);
		srb.execute().actionGet().getHits().forEach(hit -> {
			results.add(hit.getFields());
		});
		return results;
	}

}
