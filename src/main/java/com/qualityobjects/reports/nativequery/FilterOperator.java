package com.qualityobjects.reports.nativequery;

import lombok.Getter;

public enum FilterOperator {
  /**
   * Exists subselect
   */
  EXISTS("exists"),
  /**
   * Less than and operator.
   */
  LESS_THAN("<"),
  /**
   * Less than or equal and operator.
   */
  LESS_THAN_OR_EQUAL("<="),
  /**
   * Equal and operator.
   */
  EQUAL("="),
  /**
   * Not equal and operator.
   */
  NOT_EQUAL("!="),
  /**
   * Greater than or equal and operator.
   */
  GREATER_THAN_OR_EQUAL(">="),
  /**
   * Greater than and operator.
   */
  GREATER_THAN(">"),
  /**
   * Contains text
   */
  CONTAINS("like"),
  /**
   * Contains ignoring cases operator.
   */
  CONTAINS_IC("ilike"),
  /**
   * In and operator.
   */
  IN("in"),
  /**
   * Is null operator
   */
  IS_NULL("is null");

  @Getter
  private final String sqlOp;

  private FilterOperator(String sql) {
    this.sqlOp = sql;
  }
}
