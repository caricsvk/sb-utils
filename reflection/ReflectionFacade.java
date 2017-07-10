package milo.utils.reflection;

import milo.utils.jpa.SearchFieldsPrototype;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReflectionFacade {

	private static EnumResolver enumResolver = new EnumResolver();

	public static EnumResolver getEnumResolver() {
		return enumResolver;
	}

	public static Map<String, String> getSearchFieldsTypes(Class<?> classType) {
		HashMap<String, String> types = new HashMap<>();
		SearchFieldsPrototype.SearchFields searchFields = SearchFieldsPrototype.getSearchFields(classType);
		for (Field field : searchFields.getAllFields()) {
			types.put(field.getName(), field.getGenericType().toString());
		}
		return types;
	}

	public static List<Field> getFieldsTypes(Class entityClass) {
		Class<?> currentEntityClass = entityClass;
		List<Field> allFields = new ArrayList<>();
		do {
			for (Field field : currentEntityClass.getDeclaredFields()) {
				allFields.add(field);
			}
			currentEntityClass = currentEntityClass.getSuperclass();
		} while (currentEntityClass != null);
		return allFields;
	}

	public static Map<String, Object> getKeyValues(Object object) {
		return getKeyValues(object, object.getClass().getSimpleName() + ".", new ArrayList<>());
	}

	private static Map<String, Object> getKeyValues(Object object, String prefix, List<Object> processedObjects) {
		HashMap<String, Object> result = new HashMap<>();
		List<Field> fields = ReflectionFacade.getFieldsTypes(object.getClass());

		for (Field field : fields) {
			field.setAccessible(true);
			Object fieldValue = null;
			try {
				fieldValue = field.get(object);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}

			// TODO collections
			if (field.getGenericType().getTypeName().startsWith("java")) {
				result.put(prefix + field.getName(), fieldValue);
			} else if (fieldValue != null && !processedObjects.contains(fieldValue)) {
				processedObjects.add(fieldValue);
				result.putAll(getKeyValues(fieldValue, prefix + field.getName() + ".", processedObjects));
			}
		}
		return result;
	}
}
