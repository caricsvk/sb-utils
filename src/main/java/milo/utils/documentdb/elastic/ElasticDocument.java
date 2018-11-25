package milo.utils.documentdb.elastic;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
public @interface ElasticDocument {
    String index();
    String type();
}
