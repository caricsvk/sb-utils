package milo.utils.reflection;

import java.util.HashMap;
import java.util.Map;

public class EnumResolver {

	public Object[] getEnumValues(String fullClassName) {
		if (fullClassName == null) {
			return new Object[0];
		}
		try {
			Class<?> clazz = Class.forName(fullClassName);
			return clazz.getEnumConstants();
		} catch (ClassNotFoundException ex) {
			return new Object[0];
		}
	}

	public boolean isEnum(String fullClassName) {
		if (fullClassName == null) {
			return false;
		}
		try {
			Class<?> clazz = Class.forName(fullClassName);
			return (Enum.class.isAssignableFrom(clazz));
		} catch (ClassNotFoundException ex) {
			return false;
		}
	}

	public Map<String, Object> isEnumAsEntry(String fullClassName) {
		HashMap<String, Object> map = new HashMap<>();
		map.put("key", "isEnum");
		map.put("value", ReflectionFacade.getEnumResolver().isEnum(fullClassName));
		return map;
	}

}
