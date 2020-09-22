package com.qualityobjects.reports.nativequery;

import com.qualityobjects.commons.exception.QORuntimeException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SubqueryCondition implements Condition {

  private final FilterOperator op;
  private final String subQuery;
  private final Condition subQueryWhere;

  public static SubqueryCondition of(FilterOperator op, String subQuery, Condition subQueryWhere) {
    if (op != FilterOperator.EXISTS) {
      throw new QORuntimeException("SubqueryCondition must be used with EXISTS or NOT_EXISTS operators.");
    }
    return new SubqueryCondition(op, subQuery, subQueryWhere);
  }

  @Override
  public JdbcTemplateSQLWhere toJdbcTemplateSQLWhere() {
	  JdbcTemplateSQLWhere mbwc = subQueryWhere.toJdbcTemplateSQLWhere();
    String sqlTemplate =
        String.format("(%s (%s))", op.getSqlOp(), SQL.subqueryTemplate(subQuery, mbwc));

    return JdbcTemplateSQLWhere.of(sqlTemplate, mbwc.getParams());
  }
}
