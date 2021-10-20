package com.qualityobjects.reports.nativequery;

import lombok.Data;
import lombok.RequiredArgsConstructor;


@Data
@RequiredArgsConstructor(staticName = "of")
public class RawSqlCondition implements Condition {

  private final String rawSql;

  @Override
  public JdbcTemplateSQLWhere toMyBatisCondition() {
    return JdbcTemplateSQLWhere.of(rawSql, Map.of());
  }
}
