package dev.unmango;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.javaoperatorsdk.operator.junit.LocallyRunOperatorExtension;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;

class BlockyReconcilerIntegrationTest {

  public static final String RESOURCE_NAME = "test1";
  public static final String INITIAL_IMAGE = "ghcr.io/0xerr0r/blocky:v0.24";
  public static final String UPDATED_IMAGE = "ghcr.io/0xerr0r/blocky:v0.25";

  @RegisterExtension
  LocallyRunOperatorExtension extension =
      LocallyRunOperatorExtension.builder().withReconciler(BlockyReconciler.class).build();

  @Test
  void createsDeployment() {
    extension.create(testResource());

    await()
        .atMost(Duration.ofMinutes(3))
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

              assertThat(deployment.getStatus().getReadyReplicas()).isGreaterThanOrEqualTo(1);
            });
  }

  @Test
  void usesExistingConfigMap() {
    var existingCm =
        new ConfigMapBuilder()
            .withMetadata(new ObjectMetaBuilder().withName("my-config").build())
            .withData(Map.of(ConfigMapDependentResource.CONFIG_KEY, "upstreams:\n  groups:\n    default:\n      - 8.8.8.8\n"))
            .build();
    extension.create(existingCm);

    var resource = testResource();
    var config = new BlockyConfig();
    config.setConfigMap("my-config");
    resource.getSpec().setConfig(config);
    extension.create(resource);

    await()
        .atMost(Duration.ofMinutes(3))
        .untilAsserted(
            () -> {
              assertThat(extension.get(ConfigMap.class, RESOURCE_NAME + "-config")).isNull();

              var deployment = extension.get(Deployment.class, RESOURCE_NAME);
              assertThat(deployment).isNotNull();
              var volumes = deployment.getSpec().getTemplate().getSpec().getVolumes();
              assertThat(volumes).hasSize(1);
              assertThat(volumes.get(0).getConfigMap().getName()).isEqualTo("my-config");

              assertThat(deployment.getStatus().getReadyReplicas()).isGreaterThanOrEqualTo(1);
            });
  }

  @Test
  void testCRUDOperations() {
    var cr = extension.create(testResource());

    await()
        .atMost(Duration.ofMinutes(3))
        .untilAsserted(
            () -> {
              var deployment = extension.get(Deployment.class, RESOURCE_NAME);
              assertThat(deployment).isNotNull();
              assertThat(deployment.getSpec().getTemplate().getSpec().getContainers())
                  .hasSize(1)
                  .first()
                  .satisfies(c -> assertThat(c.getImage()).isEqualTo(INITIAL_IMAGE));
              assertThat(deployment.getStatus().getReadyReplicas()).isGreaterThanOrEqualTo(1);
            });

    cr.getSpec().setImage(UPDATED_IMAGE);
    cr = extension.replace(cr);

    await()
        .atMost(Duration.ofMinutes(3))
        .untilAsserted(
            () -> {
              var deployment = extension.get(Deployment.class, RESOURCE_NAME);
              assertThat(deployment.getSpec().getTemplate().getSpec().getContainers())
                  .first()
                  .satisfies(c -> assertThat(c.getImage()).isEqualTo(UPDATED_IMAGE));
              assertThat(deployment.getStatus().getReadyReplicas()).isGreaterThanOrEqualTo(1);
            });

    extension.delete(cr);

    await()
        .untilAsserted(
            () -> {
              var deployment = extension.get(Deployment.class, RESOURCE_NAME);
              assertThat(deployment).isNull();
            });
  }

  @Test
  void resolvesDnsQuery() throws Exception {
    var cm =
        new ConfigMapBuilder()
            .withMetadata(new ObjectMetaBuilder().withName("upstream-config").build())
            .withData(
                Map.of(
                    ConfigMapDependentResource.CONFIG_KEY,
                    "upstreams:\n  groups:\n    default:\n      - 8.8.8.8\n"))
            .build();
    extension.create(cm);

    var resource = testResource();
    var config = new BlockyConfig();
    config.setConfigMap("upstream-config");
    resource.getSpec().setConfig(config);
    extension.create(resource);

    var client = extension.getKubernetesClient();
    var namespace = extension.getNamespace();

    await()
        .atMost(Duration.ofMinutes(3))
        .untilAsserted(
            () -> {
              var deployment = extension.get(Deployment.class, RESOURCE_NAME);
              assertThat(deployment).isNotNull();
              assertThat(deployment.getStatus()).isNotNull();
              assertThat(deployment.getStatus().getReadyReplicas()).isGreaterThanOrEqualTo(1);
            });

    var pods =
        client
            .pods()
            .inNamespace(namespace)
            .withLabel("app.kubernetes.io/name", "blocky")
            .list()
            .getItems();
    assertThat(pods).isNotEmpty();
    var podName = pods.get(0).getMetadata().getName();

    try (var portForward =
        client.pods().inNamespace(namespace).withName(podName).portForward(53)) {
      var resolver = new SimpleResolver("127.0.0.1");
      resolver.setPort(portForward.getLocalPort());
      resolver.setTCP(true);

      var query = Message.newQuery(Record.newRecord(Name.fromString("example.com."), Type.A, DClass.IN));
      var response = resolver.send(query);

      assertThat(response.getSection(Section.ANSWER)).isNotEmpty();
    }
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
