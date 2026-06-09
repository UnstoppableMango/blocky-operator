package dev.unmango;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class OperatorManagedConfigCondition implements Condition<ConfigMap, Blocky> {

  @Override
  public boolean isMet(
      DependentResource<ConfigMap, Blocky> dependentResource,
      Blocky primary,
      Context<Blocky> context) {
    var config = primary.getSpec().getConfig();
    return config != null
        && config.getConfigMapRef() == null
        && (config.getYaml() != null || config.getInline() != null);
  }
}
