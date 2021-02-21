package com.qualityobjects.reports.common;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.stream.Collectors;

import com.qualityobjects.commons.exception.QORuntimeException;
import com.qualityobjects.commons.utils.JsonUtils;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.data.convert.ReadingConverter;

import lombok.AllArgsConstructor;

/**
 * Generic converter for json columns in specific Java classes
 */
  @ReadingConverter
  @AllArgsConstructor
  public class JsonColumnGenericConverter implements GenericConverter {
    private final Set<Class<?>> targetClasses;
    private final Class<?> sourceClass;

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
      return targetClasses.parallelStream().map(k -> new ConvertiblePair(sourceClass, k))
          .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
      if (source == null) {
        return source;
      }

      String jsonStr = null;
      try {
        if (source instanceof byte[]) {
          // This is for H2 DB
          jsonStr = new String(byte[].class.cast(source), Charset.forName("utf8"));
        } else {
          jsonStr = source.toString();
        }
        return JsonUtils.parseJSON(jsonStr, targetType.getObjectType());
      } catch (IOException e) {
        throw new QORuntimeException("Error parsing JSON column", e);
      }
    }
  }