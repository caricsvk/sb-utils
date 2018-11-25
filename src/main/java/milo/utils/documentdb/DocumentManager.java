package milo.utils.documentdb;

import java.util.List;

public interface DocumentManager {
    
    public String insert(Object o);
    public void update(DocumentEntity o);
    public void update(DocumentEntity o, String market);
    public <T extends DocumentEntity> T get(Class<T> documentClass, Object id);
    public <T extends DocumentEntity> T getFromIndex(Class<T> documentClass, Object id, String index);
    public <T extends DocumentEntity> List<T> find(Class<T> documentClass, DocumentSearchQuery dsr);
    public <T extends DocumentEntity> List<T> findInIndex(Class<T> documentClass, DocumentSearchQuery dsr, String index);
    public Long findCount(Class<?> documentClass, DocumentSearchQuery dsr);
    public void delete(Class<?> documentClass, Object id);
   
}