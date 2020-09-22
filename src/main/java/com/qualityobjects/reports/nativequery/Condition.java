package com.qualityobjects.reports.nativequery;

public interface Condition {

  public JdbcTemplateSQLWhere toJdbcTemplateSQLWhere();
 
}
