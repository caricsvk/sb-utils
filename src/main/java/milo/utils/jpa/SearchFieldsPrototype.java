package milo.utils.jpa;

import jakarta.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SearchFieldsPrototype {

	private static Map<Class, SearchFields> searchFields = new ConcurrentHashMap<>();

	public static SearchFields getSearchFields(Class entityClass) {
		searchFields.putIfAbsent(entityClass, new SearchFields(entityClass));
		return searchFields.get(entityClass);
	}

	public static class SearchFields {

		private List<Field> allFields = new ArrayList<>();
		private List<Field> allowedFields = new ArrayList<>();

		private SearchFields(Class entityClass) {
			Class<?> currentEntityClass = entityClass;
			do {
				for (Field field : currentEntityClass.getDeclaredFields()) {

					if (field.getName().equals("serialVersionUID") || Modifier.isTransient(field.getModifiers())
							|| Modifier.isStatic(field.getModifiers())) {
						continue;
					}

					boolean store = true;
					for (Annotation annotation : field.getDeclaredAnnotations()) {
						if (annotation instanceof XmlTransient || annotation instanceof Transient) {
							store = false;
							break;
						}
					}

					if (store) {
						allFields.add(field);
						if (isTypeAllowed(field.getType())) {
							allowedFields.add(field);
						}
					}
				}
				currentEntityClass = currentEntityClass.getSuperclass();
			} while (currentEntityClass != null);
		}

		public List<Field> getAllFields() {
			return allFields;
		}

		public List<Field> getAllowedFields() {
			return allowedFields;
		}

		public Field getField(String name) {
			for (Field field : allFields) {
				if (field.getName().equals(name)) {
					return field;
				}
			}
			return null;
		}

		public static Boolean isTypeAllowed(Class<?> clazz) {
			// Logger.getAnonymousLogger().info("checking type : " + clazz.getName());
			if (clazz.isPrimitive() || getAllowedTypes().contains(clazz)) {
				return true;
			}
			Set<Class<?>> allowedTypes = getAllowedTypes();
			for (Class<?> allowedType : allowedTypes) {
				if (allowedType.isAssignableFrom(clazz)) {
					return true;
				}
			}
			return false;
		}

		public static Set<Class<?>> getAllowedTypes() {
			Set<Class<?>> ret = new HashSet<>();
			ret.add(Boolean.class);
			ret.add(Character.class);
			ret.add(Byte.class);
			ret.add(Short.class);
			ret.add(Integer.class);
			ret.add(Long.class);
			ret.add(Float.class);
			ret.add(Double.class);
			ret.add(Void.class);
			ret.add(String.class);
			ret.add(Date.class);
			ret.add(Calendar.class);
			ret.add(Timestamp.class);
			ret.add(BigDecimal.class);
			// this should not be allowed because we cannot search directly and
			// type safe in this types
			// ret.add(Collection.class);
			ret.add(Enum.class);
			return ret;
		}
	}
}
