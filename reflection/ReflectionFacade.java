package milo.utils.reflection;

import milo.utils.jpa.SearchFieldsPrototype;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class ReflectionFacade {

	private static EnumResolver enumResolver = new EnumResolver();

	public static EnumResolver getEnumResolver() {
		return enumResolver;
	}

	public static Map<String, String> getFieldsTypes(Class<?> classType) {
		HashMap<String, String> types = new HashMap<>();
		SearchFieldsPrototype.SearchFields searchFields = SearchFieldsPrototype.getSearchFields(classType);
		for (Field field : searchFields.getAllFields()) {
			types.put(field.getName(), field.getGenericType().toString());
		}
		return types;
	}
}
