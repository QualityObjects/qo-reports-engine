package com.qualityobjects.reports.nativequery.mapper;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.qualityobjects.commons.exception.QORuntimeException;
import com.qualityobjects.commons.utils.JsonUtils;

import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

public class CustomBeanRowMapper<T> extends BeanPropertyRowMapper<T> {

    public static final Class<? extends Annotation> JSON_TYPE_ANNOTATION = JsonAutoDetect.class;

    public static <K> CustomBeanRowMapper<K> of(Class<K> beanType) {
        return new CustomBeanRowMapper<>(beanType);
    }

    private CustomBeanRowMapper(Class<T> beanType) {
        super(beanType, false);
    }

    @Override
    protected Object getColumnValue(ResultSet rs, int index, PropertyDescriptor pd) throws SQLException {
        return processPossibleJsonColumn(super.getColumnValue(rs, index, pd), pd.getPropertyType());
    }

    @Override
    protected Object getColumnValue(ResultSet rs, int index, Class<?> paramType) throws SQLException {
        return processPossibleJsonColumn(super.getColumnValue(rs, index, paramType), paramType);
    }

    private Object processPossibleJsonColumn(Object rawValue, Class<?> targetType) {
        if (rawValue == null || !(rawValue instanceof PGobject) || targetType.equals(rawValue.getClass())) {
            return rawValue;
        }
        boolean convertToJson = (targetType.getAnnotation(JSON_TYPE_ANNOTATION) != null);

        if (convertToJson) {
            String jsonStr = null;
            try {
                if (rawValue instanceof byte[]) {
                    // This is for H2 DB
                    jsonStr = new String(byte[].class.cast(rawValue), Charset.forName("utf8"));
                } else {
                    jsonStr = rawValue.toString();
                }
                return JsonUtils.parseJSON(jsonStr, targetType);
            } catch (IOException e) {
                throw new QORuntimeException("Error parsing JSON column", e);
            }
        }

        return rawValue;
    }
}
