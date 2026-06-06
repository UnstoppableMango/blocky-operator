package dev.unmango;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.javaoperatorsdk.operator.junit.LocallyRunOperatorExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class BlockyReconcilerIntegrationTest {

  public static final String RESOURCE_NAME = "test1";
  public static final String INITIAL_IMAGE = "ghcr.io/0xERR0R/blocky:v0.24";
  public static final String UPDATED_IMAGE = "ghcr.io/0xERR0R/blocky:v0.25";

  @RegisterExtension
  LocallyRunOperatorExtension extension =
      LocallyRunOperatorExtension.builder().withReconciler(BlockyReconciler.class).build();

  @Test
  void createsDeployment() {
    extension.create(testResource());

    await()
        .untilAsserted(
            () -> {
              var configMap = extension.get(ConfigMap.class, RESOURCE_NAME + "-config");
              assertThat(configMap).isNotNull();
              assertThat(configMap.getData()).containsKey(ConfigMapDependentResource.CONFIG_KEY);

              var deployment = extension.get(Deployment.class, RESOURCE_NAME);
              assertThat(deployment).isNotNull();
              var podSpec = deployment.getSpec().getTemplate().getSpec();
              assertThat(podSpec.getContainers())
                  .hasSize(1)
                  .first()
                  .satisfies(
                      c -> {
                        assertThat(c.getImage()).isEqualTo(INITIAL_IMAGE);
                        assertThat(c.getVolumeMounts()).hasSize(1);
                        assertThat(c.getVolumeMounts().get(0).getMountPath())
                            .isEqualTo(ConfigMapDependentResource.CONFIG_MOUNT_PATH);
                        assertThat(c.getVolumeMounts().get(0).getReadOnly()).isTrue();
                      });
              assertThat(podSpec.getVolumes()).hasSize(1);
              assertThat(podSpec.getVolumes().get(0).getName())
                  .isEqualTo(ConfigMapDependentResource.CONFIG_VOLUME_NAME);
              assertThat(podSpec.getVolumes().get(0).getConfigMap().getName())
                  .isEqualTo(RESOURCE_NAME + "-config");
            });
  }

  @Test
  void usesExistingConfigMap() {
    var existingCm =
        new ConfigMapBuilder()
            .withMetadata(new ObjectMetaBuilder().withName("my-config").build())
            .withData(Map.of(ConfigMapDependentResource.CONFIG_KEY, "upstream:\n  default: 8.8.8.8\n"))
            .build();
    extension.create(existingCm);

    var resource = testResource();
    var config = new BlockyConfig();
    config.setConfigMap("my-config");
    resource.getSpec().setConfig(config);
    extension.create(resource);

    await()
        .untilAsserted(
            () -> {
              assertThat(extension.get(ConfigMap.class, RESOURCE_NAME + "-config")).isNull();

              var deployment = extension.get(Deployment.class, RESOURCE_NAME);
              assertThat(deployment).isNotNull();
              var volumes = deployment.getSpec().getTemplate().getSpec().getVolumes();
              assertThat(volumes).hasSize(1);
              assertThat(volumes.get(0).getConfigMap().getName()).isEqualTo("my-config");
            });
  }

  @Test
  void testCRUDOperations() {
    var cr = extension.create(testResource());

    await()
        .untilAsserted(
            () -> {
              var deployment = extension.get(Deployment.class, RESOURCE_NAME);
              assertThat(deployment).isNotNull();
              assertThat(deployment.getSpec().getTemplate().getSpec().getContainers())
                  .hasSize(1)
                  .first()
                  .satisfies(c -> assertThat(c.getImage()).isEqualTo(INITIAL_IMAGE));
            });

    cr.getSpec().setImage(UPDATED_IMAGE);
    cr = extension.replace(cr);

    await()
        .untilAsserted(
            () -> {
              var deployment = extension.get(Deployment.class, RESOURCE_NAME);
              assertThat(deployment.getSpec().getTemplate().getSpec().getContainers())
                  .first()
                  .satisfies(c -> assertThat(c.getImage()).isEqualTo(UPDATED_IMAGE));
            });

    extension.delete(cr);

    await()
        .untilAsserted(
            () -> {
              var deployment = extension.get(Deployment.class, RESOURCE_NAME);
              assertThat(deployment).isNull();
            });
  }

  Blocky testResource() {
    var resource = new Blocky();
    resource.setMetadata(new ObjectMetaBuilder().withName(RESOURCE_NAME).build());
    var spec = new BlockySpec();
    spec.setImage(INITIAL_IMAGE);
    resource.setSpec(spec);
    return resource;
  }
}
