package milo.utils.documentdb.elastic;

import milo.utils.documentdb.DocumentEntity;
import milo.utils.documentdb.DocumentJsonSerializer;
import milo.utils.documentdb.DocumentManager;
import milo.utils.documentdb.DocumentSearchQuery;
import milo.utils.jpa.search.EntityFilter;
import milo.utils.jpa.search.EntityFilterType;
import milo.utils.jpa.search.OrderType;
import org.apache.http.HttpHost;
import org.apache.lucene.queryparser.xml.builders.BooleanQueryBuilder;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.document.DocumentField;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchPhraseQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.SpanTermQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.lang.annotation.AnnotationFormatError;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class ElasticDocumentManager implements DocumentManager {

	private static final Logger LOG = Logger.getLogger(ElasticDocumentManager.class.getName());

	private static final String ALL = "_all";
	private RestHighLevelClient client;

	protected abstract String getElasticCluster();

	protected abstract String getElasticHost();

	protected abstract int getElasticPort();

	protected abstract String getLocaleRegion();

	protected abstract DocumentJsonSerializer getJsonSerializer();

	@PostConstruct
	public void init() {
		client = new RestHighLevelClient(RestClient.builder(
				new HttpHost(getElasticHost(), getElasticPort(), "http")
		));
	}

	@PreDestroy
	public void destroy() throws IOException {
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
//			elasticIndexType.setType(annotation.type());
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
			IndexRequest request = new IndexRequest(elasticIndexType.getIndex())
//					.type(elasticIndexType.getType())
					.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
					.source(json, XContentType.JSON);
//			IndexResponse response = client.prepareIndex(
//					elasticIndexType.getIndex(), elasticIndexType.getType()
//			).setRefresh(true).setSource(json).execute().actionGet();
			IndexResponse response = client.index(request, RequestOptions.DEFAULT);
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
//			updateRequest.type(elasticIndexType.getType());
			updateRequest.id(document.getId());
			updateRequest.doc(json, XContentType.JSON);
			client.update(updateRequest, RequestOptions.DEFAULT).getGetResult();
		} catch (IOException ex) {
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
		try {
			GetResponse response = getResponse(id, elasticIndexType);

			String json = response.getSourceAsString();
			T object = getJsonSerializer().deserialize(json, documentClass);
			object.setId(response.getId());
			return object;
		} catch (IOException ex) {
			Logger.getLogger(ElasticDocumentManager.class.getName()).log(Level.SEVERE, null, ex);
			return null;
		}

	}

	private GetResponse getResponse(Object id, ElasticIndexType elasticIndexType) throws IOException {
		GetRequest request = new GetRequest().index(elasticIndexType.getIndex()).id((String) id);
//				.type(elasticIndexType.getType()).id((String) id);
		return client.get(request, RequestOptions.DEFAULT);
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
		SearchResponse searchResponse = null;
		try {
			searchResponse = search(elasticIndexType, dsr);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		List<T> objects = new ArrayList<>();
		for (SearchHit sh : searchResponse.getHits()) {
			try {
				T object = dsr.getFields() == null || dsr.getFields().isEmpty() ?
						getJsonSerializer().deserialize(sh.getSourceAsString(), documentClass) :
						makeInstanceFromFields(documentClass, dsr.getFields(), sh.getFields());
				object.setId(sh.getId());
				objects.add(object);
			} catch (IOException | InstantiationException | NoSuchFieldException | IllegalAccessException ex) {
				Logger.getLogger(ElasticDocumentManager.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		return objects;
	}

	@Override
	public Long findCount(Class<?> documentClass, DocumentSearchQuery dsr) {
		dsr.setLimit(0);
		try {
			return search(getIndexType(documentClass), dsr).getHits().getTotalHits().value;
		} catch (IOException e) {
			e.printStackTrace();
			return -500l;
		}
	}

	private SearchResponse search(ElasticIndexType elasticIndexType, DocumentSearchQuery dsr) throws IOException {

		QueryBuilder qb = null;
		if (dsr.getFilterBuilder() == null) {
			dsr.setFilterBuilder(QueryBuilders.boolQuery());
		}

		if (dsr.getQueryBuilder() != null) {
			qb = dsr.getQueryBuilder();
		} else if (dsr.getId() != null && !dsr.getId().isEmpty()) {
//				dsr.getFilter().replaceAll("-", "\\-")
			qb = QueryBuilders.matchQuery("_id", dsr.getId());
		} else if (dsr.getFilter() != null && !dsr.getFilter().isEmpty()) {
			qb = QueryBuilders.matchQuery("_all", dsr.getFilter());
		} else {
			for (Map.Entry<String, EntityFilter> entry : dsr.getFilterParameters().entrySet()) {
				if (EntityFilterType.WILDCARD.equals(entry.getValue().getEntityFilterType())) {
					qb = QueryBuilders.matchQuery(entry.getValue().getFieldName(),
							entry.getValue().getValue().toLowerCase());
				} else {
					dsr.getFilterBuilder().filter().add(createPredicates(entry.getValue()));
				}
			}
		}

		if (dsr.getScrollId() != null) { // TODO
//			SearchResponse searchResponse = client.prepareSearchScroll(dsr.getScrollId())
//					.setScroll(new TimeValue(dsr.getScroll())).execute().actionGet();
//			dsr.setScrollId(searchResponse.getScrollId());
//			return searchResponse;
			return null;
		} else {

			if (qb != null) {
				dsr.getFilterBuilder().must(qb);
			}

			SearchSourceBuilder srb = new SearchSourceBuilder()
					.query(dsr.getQueryBuilder())
					.version(Boolean.TRUE)
					.query(dsr.getFilterBuilder());

			if (dsr.getLimit() != null && dsr.getLimit() > 0) {
				srb.size(dsr.getLimit());
			}
			if (dsr.getOffset() != null && dsr.getOffset() > 0) {
				srb.from(dsr.getOffset());
			}
			if (dsr.getOrder() != null && !(dsr.getOrder().isEmpty())) {
				FieldSortBuilder fsb = SortBuilders.fieldSort(dsr.getOrder());
				if (dsr.getOrderType().equals(OrderType.ASC)) {
					fsb.order(SortOrder.ASC);
				} else {
					fsb.order(SortOrder.DESC);
				}
				srb.sort(fsb);
			}
			if (dsr.getFields() != null && !dsr.getFields().isEmpty()) {
				srb.fetchSource(false);
				dsr.getFields().forEach(field -> srb.fetchField(field));
			}

			SearchRequest request = new SearchRequest(elasticIndexType.getIndex())
					.source(srb);
//					.types(elasticIndexType.getType());

			if (dsr.getScroll() != null && dsr.getScroll() > 0) {
				request.scroll(new TimeValue(dsr.getScroll()));
//				request.setSearchType(SearchType.SCAN);
			}

			SearchResponse searchResponse = client.search(request, RequestOptions.DEFAULT);
			System.out.println("ElasticDocumentManager.search: " + srb.toString());
			dsr.setScrollId(searchResponse.getScrollId()); //in case of scrolling

			return searchResponse;
		}
	}

	private QueryBuilder createPredicates(EntityFilter entityFilter) {
		switch (entityFilter.getEntityFilterType()) {
			case EXACT_NOT:
			case EXACT:
				List<String> lowerCaseValues = entityFilter.getValues().stream()
						.map(String::toLowerCase).collect(Collectors.toList());
				QueryBuilder query;
				if (lowerCaseValues.size() == 1) {
					query = QueryBuilders.matchPhraseQuery(entityFilter.getFieldName(), lowerCaseValues.get(0));
				} else {
					BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
					lowerCaseValues.forEach(value -> boolQuery.should().add(
							QueryBuilders.matchPhraseQuery(entityFilter.getFieldName(), value)
					));
					query = boolQuery;
				}
				return EntityFilterType.EXACT.equals(entityFilter.getEntityFilterType()) ? query :
						QueryBuilders.boolQuery().mustNot(query);
			case EMPTY:
				return QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(entityFilter.getFieldName()));
			case MIN:
				return QueryBuilders.rangeQuery(entityFilter.getFieldName()).from(entityFilter.getFirstValue());
			case MAX:
				return QueryBuilders.rangeQuery(entityFilter.getFieldName()).to(entityFilter.getFirstValue());
			case WILDCARD:
				return QueryBuilders.matchQuery(entityFilter.getFieldName(), entityFilter.getValue().toLowerCase());
			case MIN_MAX:
				return QueryBuilders.rangeQuery(entityFilter.getFieldName())
							.from(entityFilter.getFirstValue())
							.to(entityFilter.getSecondValue());
			default:
				throw new IllegalStateException("Unexpected value: " + entityFilter.getEntityFilterType());
		}
	}

	@Override
	public void delete(Class<?> documentClass, Object id) {
		ElasticIndexType elasticIndexType = getIndexType(documentClass);
		DeleteRequest deleteRequest = new DeleteRequest(elasticIndexType.getIndex()).id((String) id);
//				.type(elasticIndexType.getType()).id((String) id);
		try {
			DeleteResponse delete = client.delete(deleteRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public <T extends DocumentEntity> Aggregations aggregations(Class<T> documentClass, AbstractAggregationBuilder aab) {
		return aggregations(documentClass, aab, null);
	}

	public <T extends DocumentEntity> Aggregations aggregations(Class<T> documentClass, AbstractAggregationBuilder aab,
	                                                            QueryBuilder queryBuilder) {
//		ElasticIndexType elasticIndexType = getIndexType(documentClass);
//		SearchRequestBuilder srb = client.prepareSearch(elasticIndexType.getIndex());
//		srb.setSize(0);
//		srb.setTypes(elasticIndexType.getType());
//		srb.setSearchType(SearchType.COUNT);
//		srb.addAggregation(aab);
//		srb.setQuery(queryBuilder);
////		System.out.println("ElasticDocumentManager.aggregations ---- " + srb.toString());
//		return srb.execute().actionGet().getAggregations();
		return null; // TODO
	}

//	public <T extends DocumentEntity> List<Map<String, SearchHitField>> getFields(Class<T> lookInClass,
//	                                                                              FilterBuilder filterBuilder,
//	                                                                              List<String> fields,
//	                                                                              int limit) {
//		ElasticIndexType elasticIndexType = getIndexType(lookInClass);
//		SearchRequestBuilder srb = client.prepareSearch(elasticIndexType.getIndex());
//		srb.setSize(limit);
//		srb.setTypes(elasticIndexType.getType());
//		srb.setPostFilter(filterBuilder);
//		fields.forEach(srb::addField);
//		List<Map<String, SearchHitField>> results = new ArrayList<>();
////		System.out.println("ElasticDocumentManager.getFields -------- " + srb);
//		srb.execute().actionGet().getHits().forEach(hit -> {
//			results.add(hit.getFields());
//		});
//		return results;
//	}

	private <T> T makeInstanceFromFields(Class<T> documentClass, List<String> fieldNames, Map<String, DocumentField> fields)
			throws IllegalAccessException, InstantiationException, NoSuchFieldException {
		T object = documentClass.newInstance();
		for (String fieldName : fieldNames) {
			Field field = documentClass.getDeclaredField(fieldName);
			field.setAccessible(true);
			Object value = fields.get(fieldName) != null ? fields.get(fieldName).getValue() : null;
			if (value != null && BigDecimal.class.isAssignableFrom(field.getType())) {
				value = BigDecimal.valueOf(Long.class.isAssignableFrom(value.getClass()) ? (Long) value :
						Double.class.isAssignableFrom(value.getClass()) ? (Double) value : (Integer) value);
			} else if (value != null && field.getType().isEnum()) {
				value = Enum.valueOf(field.getType().asSubclass(Enum.class), (String) value);
			}
			field.set(object, value);
		}
		return object;
	}

}
