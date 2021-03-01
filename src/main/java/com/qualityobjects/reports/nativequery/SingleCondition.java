package com.qualityobjects.reports.nativequery;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.SqlParameterValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Data
@RequiredArgsConstructor(staticName = "of")
public class SingleCondition implements Condition {

  private final String field;
  private final FilterOperator op;
  private final Object value;
  /**
   * Necessary when we use the same field with different values in the same where.
   * It's not final because is modifed in the builder
   */
  private Integer suffix = 0;

  @Override
  public JdbcTemplateSQLWhere toJdbcTemplateSQLWhere() {
    String sqlTemplate = SQL.sqlFilterTemplate(this);
    List<SqlParameterValue> params;
    if (op == FilterOperator.IN) {
      params = setSingleParamValues();
    } else {
      params = List.of(SQL.paramOf(SQL.normalizeFieldParam(field, suffix), value));
    }
    return JdbcTemplateSQLWhere.of(sqlTemplate, params);
  }

  /**
   * All the values in a collection should be set individually for an <code>IN</code> clause
   * @return A list with all values in the collection as SQLValueParaams
   */
  private List<SqlParameterValue> setSingleParamValues() {
    List<SqlParameterValue> params = new ArrayList<>();
    Collection<?> coll;
    if (value.getClass().isArray())  {
      coll = List.of((Object[])value);
    } else {
      coll = Collection.class.cast(value);
    }
    int pos = 0;
    for (Object singleValue : coll) {
      params.add(SQL.paramOf(SQL.normalizeFieldParam(field, suffix, pos++), singleValue));
    }
    return params;
  }

}
