package dev.unmango;

import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.Workflow;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;

@Workflow(
    dependents = {
      @Dependent(
          type = ConfigMapDependentResource.class,
          name = "ConfigMapDependentResource",
          activationCondition = OperatorManagedConfigCondition.class),
      @Dependent(type = DeploymentDependentResource.class)
    })
public class BlockyReconciler implements Reconciler<Blocky> {

  public UpdateControl<Blocky> reconcile(Blocky primary, Context<Blocky> context) {
    return UpdateControl.noUpdate();
  }
}
