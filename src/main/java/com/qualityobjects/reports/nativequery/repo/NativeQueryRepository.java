package com.qualityobjects.reports.nativequery.repo;


import com.qualityobjects.reports.nativequery.Condition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.stream.Stream;

/**
 * "Magic" repository to provide dynamic query capabilities for native queries on Dto projections
 *
 * @author Siroco Team [siroco@qualityobjects.com]
 * @since 1.0.0
 */
public interface NativeQueryRepository {

  <T> Page<T> findAll(Condition where, Pageable pagRequest, Class<T> dtoClass);

  <T> List<T> findAll(Condition where, Class<T> dtoClass);

  <T> List<T> findAll(Condition where, Sort sort, Class<T> dtoClass);

  <T> Stream<T> findAllAsStream(Condition where, Class<T> dtoClass);

  <T> Stream<T> findAllAsStream(Condition where, Sort sort, Class<T> dtoClass);
}
