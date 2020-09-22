package com.qualityobjects.reports.nativequery;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for Condition objects.
 * It's simialr to QuerySpecificationBuilder.
 * 
 * @author Rob
 */
@Data
@RequiredArgsConstructor(staticName = "builder")
public class ConditionsBuilder {

  private final List<Condition> conditions = new ArrayList<>();

  public ConditionsBuilder add(Condition cond) {
    if (cond instanceof SingleCondition) {
      ((SingleCondition) cond).setSuffix(conditions.size());
    }
    conditions.add(cond);
    return this;
  }

  public ConditionsBuilder add(List<Condition> conds, LogicalOperator op) {
    conditions.add(MultipleCondition.of(op, conds));
    return this;
  }

  public Condition build() {
    return build(LogicalOperator.AND);
  }

  public Condition build(LogicalOperator op) {
    return MultipleCondition.of(op, conditions);
  }

}
