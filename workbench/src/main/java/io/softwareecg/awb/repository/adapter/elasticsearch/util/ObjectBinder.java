package io.softwareecg.awb.repository.adapter.elasticsearch.util;

import co.elastic.clients.elasticsearch.core.search.Hit;
import io.softwareecg.awb.repository.persistence.schema.FieldReader;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Maps Java objects to/from ElasticSearch documents.
 *
 * @since 22.09.2018
 */
public class ObjectBinder {

    public static Map<String, Object> mapToContentMap(Object ekgEntity) {

        Map<String, Field> fieldMap = FieldReader.getSchemaFields(ekgEntity.getClass());

        if (fieldMap.isEmpty()) {
            throw new IllegalStateException("class "+ ekgEntity.getClass().getName() + " does not define any fields.");
        }

        Map<String, Object> jsonMap = new HashMap<>();

        for (Map.Entry<String, Field> entry : fieldMap.entrySet()) {
            try {
                Object value = FieldUtils.readField(entry.getValue(), ekgEntity, true);

                if (value != null) {

                    if (value instanceof Date) {
                        jsonMap.put(entry.getKey(), ((Date) value).getTime());
                    } else {
                        jsonMap.put(entry.getKey(), value);
                    }
                }

            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Unable to read field '" + entry.getValue().getName()
                        + "' of  class"+ ekgEntity.getClass().getName() + "' because access is forbidden.", e);
            }
        }

        return jsonMap;
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> mapToBeans(Class<T> type, List<Hit<Map>> hits) {
        try {
            Map<String, Field> fieldMap = FieldReader.getSchemaFields(type);

            List<T> resultList = new ArrayList<>();

            for (Hit<Map> document : hits) {
                T bean = ConstructorUtils.invokeConstructor(type);
                Map<String, Object> documentFieldMap = document.source();

                if (documentFieldMap == null) {
                    continue;
                }

                for (Map.Entry<String, Field> beanFieldEntry : fieldMap.entrySet()) {

                    String fieldName = beanFieldEntry.getKey();
                    Field beanField = beanFieldEntry.getValue();

                    if (!documentFieldMap.containsKey(fieldName)) {
                         continue;
                    }

                    Object value = documentFieldMap.get(fieldName);

                    if (beanField.getType() == Date.class && value instanceof Long) {
                        value = new Date((Long) value);
                    }

                    if (beanField.getType() == byte[].class && value instanceof String) {
                        value = Base64.getDecoder().decode(value.toString());
                    }

                    FieldUtils.writeField(beanField, bean, value, true);
                }

                resultList.add(bean);
            }

            return resultList;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to map ElasticSearch result to domain entity of type '" + type.getName(), e);
        }
    }
}
