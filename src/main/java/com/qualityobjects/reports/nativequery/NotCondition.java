package com.qualityobjects.reports.nativequery;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor(staticName = "of")
public class NotCondition implements Condition {

  private final Condition innerCondition;

  @Override
  public JdbcTemplateSQLWhere toJdbcTemplateSQLWhere() {
    final String NOT_TPL = " not (%s) ";
    JdbcTemplateSQLWhere innerWhere = innerCondition.toJdbcTemplateSQLWhere();
    return JdbcTemplateSQLWhere.of(String.format(NOT_TPL, innerWhere.getWhereTemplate()), innerWhere.getParams());
  }
}
