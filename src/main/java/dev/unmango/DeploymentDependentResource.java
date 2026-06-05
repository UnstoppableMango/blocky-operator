package dev.unmango;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import java.util.Map;

@KubernetesDependent
public class DeploymentDependentResource
    extends CRUDKubernetesDependentResource<Deployment, Blocky> {

  private static final Map<String, String> LABELS = Map.of("app.kubernetes.io/name", "blocky");

  @Override
  protected Deployment desired(Blocky primary, Context<Blocky> context) {
    return new DeploymentBuilder()
        .withMetadata(
            new ObjectMetaBuilder()
                .withName(primary.getMetadata().getName())
                .withNamespace(primary.getMetadata().getNamespace())
                .build())
        .withNewSpec()
            .withNewSelector()
                .withMatchLabels(LABELS)
            .endSelector()
            .withNewTemplate()
                .withNewMetadata()
                    .withLabels(LABELS)
                .endMetadata()
                .withNewSpec()
                    .addNewContainer()
                        .withName("blocky")
                        .withImage(primary.getSpec().getImage())
                        .addNewPort()
                            .withName("dns-tcp")
                            .withContainerPort(53)
                            .withProtocol("TCP")
                        .endPort()
                        .addNewPort()
                            .withName("dns-udp")
                            .withContainerPort(53)
                            .withProtocol("UDP")
                        .endPort()
                        .addNewPort()
                            .withName("http")
                            .withContainerPort(4000)
                            .withProtocol("TCP")
                        .endPort()
                    .endContainer()
                .endSpec()
            .endTemplate()
        .endSpec()
        .build();
  }
}
