package milo.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

	public static JsonNode createJsonNode(Class clazz, int maxDeep) {

		if (maxDeep <= 0 || JsonNode.class.isAssignableFrom(clazz)) {
			return null;
		}

		ObjectMapper objectMapper = new ObjectMapper();
		ObjectNode objectNode = objectMapper.createObjectNode();

		for (Field field : clazz.getDeclaredFields()) {
			field.setAccessible(true);
			String key = field.getName();
			try {
				// is array or collection
				if (field.getType().isArray() || Collection.class.isAssignableFrom(field.getType())) {
					ParameterizedType collectionType = (ParameterizedType) field.getGenericType();
					Class<?> type = (Class<?>) collectionType.getActualTypeArguments()[0];
					ArrayNode arrayNode = objectMapper.createArrayNode();
					arrayNode.add(createJsonNode(type, maxDeep - 1));
					objectNode.replace(key, arrayNode);
				} else {
					Class<?> type = field.getType();
					if (Modifier.isStatic(field.getModifiers())) {
						continue;
					} else if (type.isEnum()) {
						objectNode.put(key, Enum.class.getName());
					} else if (!type.getName().startsWith("java") && !key.startsWith("_")) {
						objectNode.replace(key, createJsonNode(type, maxDeep - 1));
					} else {
						objectNode.put(key, type.getName());
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return objectNode;
	}
}
