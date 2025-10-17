package milo.utils.documentdb;

import java.util.List;

public interface DocumentManager {
    
    String insert(Object o);
    String insert(Object document, String id);
    void update(DocumentEntity o);
    void update(DocumentEntity o, String market);
    <T extends DocumentEntity> T get(Class<T> documentClass, Object id);
    <T extends DocumentEntity> T getFromIndex(Class<T> documentClass, Object id, String index);
    <T extends DocumentEntity> List<T> find(Class<T> documentClass, DocumentSearchQuery dsr);
    <T extends DocumentEntity> List<T> findInIndex(Class<T> documentClass, DocumentSearchQuery dsr, String index);
    Long findCount(Class<?> documentClass, DocumentSearchQuery dsr);
    void delete(Class<?> documentClass, Object id);
   
}