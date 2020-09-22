package com.qualityobjects.reports.nativequery;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 
 * @author Rob
 */
@Data
@RequiredArgsConstructor(staticName = "of")
public class MultipleCondition implements Condition {

  private final LogicalOperator op;
  private final List<Condition> conditions;

  @Override
  public JdbcTemplateSQLWhere toJdbcTemplateSQLWhere() {
    if (ObjectUtils.isEmpty(conditions)) {
      return JdbcTemplateSQLWhere.empty();
    }
    List<JdbcTemplateSQLWhere> conds =
        conditions.stream().map(Condition::toJdbcTemplateSQLWhere).collect(Collectors.toList());
   
    return JdbcTemplateSQLWhere.merge(op, conds);
  }
}
