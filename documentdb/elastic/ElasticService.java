package milo.utils.documentdb.elastic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.hppc.cursors.ObjectObjectCursor;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.mapper.MapperParsingException;
import org.elasticsearch.index.mapper.MergeMappingException;
import org.elasticsearch.search.SearchHit;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ElasticService {

	private ObjectMapper objectMapper;
	private MetaData metaData;

	@PostConstruct
	public void init() {
		objectMapper = new ObjectMapper();
		objectMapper.disable(SerializationFeature.INDENT_OUTPUT);
		objectMapper.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
	}

	//    @GET
//    @Path("delete")
//    public DeleteResponse delete(@BeanParam ElasticIdentifier elasticIdentifier) {
//        Client client = elasticIdentifier.getClient() != null ? elasticIdentifier.getClient() : defaultClient;
//        return client.prepareDelete(elasticIdentifier.getIndexOrAlias(), elasticIdentifier.getType(), elasticIdentifier.getId())
//                .setOperationThreaded(false).execute().actionGet();
//    }
//    @GET
//    @Path("copy")
//    public String copyIndex(@QueryParam("from") String fromIndex, @QueryParam("to") String toIndex) {
//        ElasticIdentifier fromEI = new ElasticIdentifier();
//        ElasticIdentifier toEI = new ElasticIdentifier();
//        fromEI.setIndex(fromIndex);
//        toEI.setIndex(toIndex);
//        copyIndex(fromEI, toEI);
//        return "OK";
//    }
	public Boolean isMappingUpToDate(ElasticIdentifier ei) {
		resolveIdentifier(ei);
		return getMapping(ei).equals(getMappingFromFile(ei));
	}

	public Integer reindex(ElasticIdentifier fromEI, ElasticIdentifier toEI) {
		String originalFromEiType = fromEI.getType();
		resolveIdentifier(fromEI);
		resolveIdentifier(toEI);

		Boolean needReindex = false;
		if (fromEI.getType() != null && !fromEI.getType().isEmpty()) {
			toEI.setType(fromEI.getType());
			//check if mapping cannot be updated without reindex
			if (!updateMappingSoft(toEI)) {
				needReindex = true;
			}
		} else {
			ImmutableOpenMap<String, MappingMetaData> mappings =
					getMetaData(fromEI.getClient()).index(fromEI.getIndex()).mappings();
			for (ObjectObjectCursor<String, MappingMetaData> mapping : mappings) {
				toEI.setType(mapping.value.type());
				//check if mapping cannot be updated without reindex
				if (!updateMappingSoft(toEI)) {
					needReindex = true;
				}
			}
		}
		if (needReindex) {
			//creates new index when operating on same EI and mapping update failed
			fromEI.setType(originalFromEiType);
			if (fromEI == toEI) {
				toEI = new ElasticIdentifier(fromEI);
				toEI.setIndex(getNewIndexName(fromEI.getIndex()));
			}
			//clone mappings to new index
			cloneMappings(fromEI, toEI);

			//copy data to new index (reindex)
			return copyIndex(fromEI, toEI);
		} else {
			return -1;
		}
	}

	public String reindexAlias(ElasticIdentifier fromEI, Boolean switchAlias, Boolean removeOld) {

		resolveIdentifier(fromEI);

		if (fromEI.getAlias() == null) {
			throw new RuntimeException("Alias is not set for index " + fromEI.getIndex());
		}

		Integer errorsCount = -10;
		errorsCount = reindex(fromEI, fromEI);
		if (errorsCount == 0 && switchAlias) {
			//update alias to newer index
			fromEI.getClient().admin().indices().prepareAliases()
					.addAlias(getNewIndexName(fromEI.getIndex()), fromEI.getAlias())
					.removeAlias(fromEI.getIndex(), fromEI.getAlias())
					.execute().actionGet();
			if (removeOld) {
				//TODO
			}
		}

		String resultMessage = "switched alias: " + switchAlias + ", removed old: " + removeOld + ", copy result: " + errorsCount;
		Logger.getLogger(ElasticService.class.getName()).log(Level.INFO, "====== ElasticService.reindex result message: {}", resultMessage);
		return resultMessage;
	}

	public TransportClient getClient(String elasticCluster, String elasticHost, int elasticPort) {
		Settings s = ImmutableSettings.settingsBuilder().put("cluster.name", elasticCluster).build();
		return new TransportClient(s).addTransportAddress(new InetSocketTransportAddress(elasticHost, elasticPort));
	}

	protected String getNewIndexName(String index) {
		String[] indexSplited = index.split("_");
		Integer version = 1;
		try {
			version = Integer.valueOf(indexSplited[indexSplited.length - 1]);
			version++;
		} catch (NumberFormatException ex) {
			indexSplited = Arrays.copyOf(indexSplited, indexSplited.length + 1);
		}
		indexSplited[indexSplited.length - 1] = version.toString();
		return join(indexSplited, "_");
	}

	protected void cloneMappings(ElasticIdentifier fromEI, ElasticIdentifier toEI) {
		ImmutableOpenMap<String, MappingMetaData> mappings = getMetaData(fromEI.getClient()).index(fromEI.getIndex()).mappings();
		for (ObjectObjectCursor<String, MappingMetaData> mapping : mappings) {
			ElasticIdentifier ei = new ElasticIdentifier(toEI);
			ei.setType(mapping.value.type());
			updateMappingSoft(ei);
		}
	}

	/**
	 * @param from
	 * @param to
	 * @return number of errors by copying
	 */
	public Integer copyIndex(ElasticIdentifier from, ElasticIdentifier to) {
		Integer errorsCount = 0;

		SearchRequestBuilder searchRequest = from.getClient().prepareSearch(from.getIndex());
		//when type is set copy only type
		if (from.getType() != null && !from.getType().isEmpty()) {
			searchRequest.setTypes(from.getType());
		}
		searchRequest.setScroll(new TimeValue(30000));
		searchRequest.setSearchType(SearchType.SCAN);
		searchRequest.setSize(50);
		SearchResponse scrollResponse = searchRequest.execute().actionGet();

		while (true) {
			int noNodeCatchesSize = 0;
			try {
				scrollResponse = from.getClient().prepareSearchScroll(scrollResponse.getScrollId()).setScroll(new TimeValue(600000)).execute().actionGet();
				SearchHit[] hits = scrollResponse.getHits().getHits();
				for (SearchHit hit : hits) {
					IndexRequestBuilder index = to.getClient().prepareIndex(to.getIndex(), hit.getType());
					index.setRefresh(true);
					index.setSource(hit.getSourceAsString());
					index.setId(hit.getId());
					try {
						index.execute().actionGet();
					} catch (MapperParsingException ex) {
						Logger.getLogger(ElasticService.class.getName()).log(Level.SEVERE, "catched mapper parsing ex for id: " + hit.getId(), ex);
						errorsCount++;
					}
				}
				//Break condition: No hits are returned
				if (scrollResponse.getHits().getHits().length == 0) {
					break;
				}
			} catch (NoNodeAvailableException ex) {
				noNodeCatchesSize++;
				try {
					Thread.sleep(1000l);
				} catch (InterruptedException ex1) {
					Logger.getLogger(ElasticService.class.getName()).log(Level.SEVERE, null, ex1);
				}
				Logger.getLogger(ElasticService.class.getName()).log(Level.SEVERE, "catched NoNodeAvailable ex for scroll id " + scrollResponse.getScrollId());
				if (noNodeCatchesSize > 15) {
					Logger.getLogger(ElasticService.class.getName()).log(Level.SEVERE, "chatches NoNodeAvailable ex for scroll id "
							+ scrollResponse.getScrollId() + " reached limit", ex);
					break;
				}
			}
		}
		return errorsCount;
	}

	public String getMapping(ElasticIdentifier ei) {
		resolveIdentifier(ei);
		try {
			String jsonOriginString = getMetaData(ei.getClient())
					.index(ei.getIndex())
					.mapping(ei.getType())
					.source()
					.string();
			JsonNode jsonNode = objectMapper.readTree(jsonOriginString);
			return objectMapper.writeValueAsString(jsonNode);
		} catch (NullPointerException | IOException ex) {
			Logger.getLogger(ElasticService.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
			return "";
		}
	}

	public String getMappingFromFile(ElasticIdentifier ei) {
		resolveIdentifier(ei);
		String filePath = "settings/mappings/" + ei.getAlias() + "_" + ei.getType() + ".json";
		try {
			JsonNode jsonNode = objectMapper.readTree(
					Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath)
			);
			return objectMapper.writeValueAsString(jsonNode);
		} catch (NullPointerException | IOException ex) {
			Logger.getLogger(ElasticService.class.getName()).log(Level.SEVERE, null, ex);
			throw new RuntimeException("Oops an error with file for mapping occured: " + filePath);
		}
	}

	protected Boolean updateMappingSoft(ElasticIdentifier ei, String mapping) {
		PutMappingRequestBuilder requestBuilder = ei.getClient().admin().indices().preparePutMapping();
		requestBuilder.setIndices(ei.getIndex());
		requestBuilder.setType(ei.getType());
		requestBuilder.setSource(mapping);
		try {
			createIndexIfNeededNoMapping(ei.getClient(), ei.getIndex());
			requestBuilder.execute().actionGet();
			return true;
		} catch (MergeMappingException ex) {
			return false;
		}
	}

	protected Boolean updateMappingSoft(ElasticIdentifier ei) {
		String mapping = getMappingFromFile(ei);
		return updateMappingSoft(ei, mapping);
	}

	protected void resolveIdentifier(ElasticIdentifier ei) {

		try {
			if (ei.getIndex() == null && getMetaData(ei.getClient()).aliases().containsKey(ei.getAlias())) {
				ei.setIndex(
						(String) getMetaData(ei.getClient()).aliases().get(ei.getAlias()).keys().toArray()[0]
				);
			}
			if (ei.getAlias() == null && getMetaData(ei.getClient()).index(ei.getIndex()) != null) {
				ei.setAlias(
						(String) getMetaData(ei.getClient()).index(ei.getIndex()).getAliases().keys().toArray()[0]
				);
			}
		} catch (Exception ex) {
			Logger.getLogger(ElasticService.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	protected void createIndexIfNeededNoMapping(Client client, String indexName) {
		try {
			// We check first if index already exists
			if (!isIndexExist(client, indexName)) {
				CreateIndexRequestBuilder cirb = client.admin().indices().prepareCreate(indexName);
				CreateIndexResponse createIndexResponse = cirb.execute().actionGet();
				if (!createIndexResponse.isAcknowledged()) {
					throw new Exception("Could not create index [" + indexName + "].");
				}
			}
		} catch (Exception ex) {
			Logger.getLogger(ElasticService.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	protected boolean isIndexExist(Client client, String index) throws Exception {
		return client.admin().indices().prepareExists(index).execute().actionGet().isExists();
	}

	protected ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	protected MetaData getMetaData(Client client) {
		if (metaData == null) {
			metaData = client.admin().cluster().prepareState().execute().actionGet().getState().getMetaData();
		}
		return metaData;
	}

	private String join(String[] array, String joiner) {
		String result = "";
		for (int i = 0; i < array.length; i++) {
			result += joiner + array[i];
		}
		return result.substring(1);
	}

}
