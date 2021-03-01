package com.qualityobjects.reports.nativequery;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor(staticName = "of")
public class JoinCondition implements Condition {

  private final String field;
  private final String joinField;

  @Override
  public JdbcTemplateSQLWhere toJdbcTemplateSQLWhere() {
    String sqlTemplate = String.format("%s = %s", field, joinField);
    return JdbcTemplateSQLWhere.of(sqlTemplate, null);
  }
}
