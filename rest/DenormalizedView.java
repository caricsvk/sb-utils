package milo.utils.rest;

import org.glassfish.hk2.api.AnnotationLiteral;
import org.glassfish.jersey.message.filtering.EntityFiltering;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@EntityFiltering
public @interface DenormalizedView {
 
    class Factory extends AnnotationLiteral<DenormalizedView> implements DenormalizedView {
 
        private Factory() {}
 
        public static DenormalizedView get() {
            return new Factory();
        }
    }
}