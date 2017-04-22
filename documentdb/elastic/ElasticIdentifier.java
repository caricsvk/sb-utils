package milo.utils.documentdb.elastic;

import org.elasticsearch.client.Client;

import javax.ws.rs.QueryParam;

public class ElasticIdentifier {
    
 private Client client;
    @QueryParam("index")
    private String index;
    @QueryParam("alias")
    private String alias;
    @QueryParam("type")
    private String type;
    @QueryParam("id")
    private String id;

    public ElasticIdentifier() {
    }

    public ElasticIdentifier(String index) {
        this.index = index;
    }

    public ElasticIdentifier(String index, String type) {
        this.index = index;
        this.type = type;
    }

    public ElasticIdentifier(String index, String alias, String type) {
        this.index = index;
        this.alias = alias;
        this.type = type;
    }
    
    public ElasticIdentifier(ElasticIdentifier ei) {
        client = ei.getClient();
        index = ei.getIndex();
        alias = ei.getAlias();
        type = ei.getType();
        id = ei.getId();
    }
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }   

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

}
