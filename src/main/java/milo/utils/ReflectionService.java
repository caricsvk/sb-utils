package milo.utils;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ReflectionService {

	public static Map createClassModel(Class clazz, int maxDeep) {
		Map map = new HashMap();
		for (Field field : clazz.getDeclaredFields()) {
			field.setAccessible(true);
			String key = field.getName();
			try {
				Class<?> type;
				if (Collection.class.isAssignableFrom(field.getType())) {
					ParameterizedType collectionType = (ParameterizedType) field.getGenericType();
					type = (Class<?>) collectionType.getActualTypeArguments()[0];
				} else {
					type = field.getType();
				}

				// System.out.println(key + "
				// ReflectionResource.createClassModel ==== putting type: " +
				// field.getType().getName() + ", static: "
				// + Modifier.isStatic(field.getModifiers()) + ", enum: " +
				// type.isEnum() + ", java.*: " +
				// type.getName().startsWith("java"));

				if (Modifier.isStatic(field.getModifiers())) {
					continue;
				} else if (type.isEnum()) {
					map.put(key, Enum.class.getName());
				} else if (!type.getName().startsWith("java") && !key.startsWith("_") && maxDeep > 0) {
					map.put(key, createClassModel(type, maxDeep - 1));
				} else {
					map.put(key, type.getName());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return map;
	}

	public static JsonObject createJsonNode(Class clazz, int maxDeep) {

		if (maxDeep <= 0 || JsonObject.class.isAssignableFrom(clazz)) {
			return null;
		}

		JsonObject objectNode = JsonValue.EMPTY_JSON_OBJECT;

		for (Field field : clazz.getDeclaredFields()) {
			field.setAccessible(true);
			String key = field.getName();
			try {
				// is array or collection
				if (field.getType().isArray() || Collection.class.isAssignableFrom(field.getType())) {
					ParameterizedType collectionType = (ParameterizedType) field.getGenericType();
					Class<?> type = (Class<?>) collectionType.getActualTypeArguments()[0];
					JsonArray arrayNode = JsonValue.EMPTY_JSON_ARRAY;
					arrayNode.add(createJsonNode(type, maxDeep - 1));
					objectNode.replace(key, arrayNode);
				} else {
					Class<?> type = field.getType();
					if (Modifier.isStatic(field.getModifiers())) {
						continue;
					} else if (type.isEnum()) {
						objectNode.put(key, Json.createValue(Enum.class.getName()));
					} else if (!type.getName().startsWith("java") && !key.startsWith("_")) {
						objectNode.replace(key, createJsonNode(type, maxDeep - 1));
					} else {
						objectNode.put(key, Json.createValue(type.getName()));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return objectNode;
	}
}
