package dev.unmango;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.javaoperatorsdk.operator.junit.LocallyRunOperatorExtension;
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
              var deployment = extension.get(Deployment.class, RESOURCE_NAME);
              assertThat(deployment).isNotNull();
              assertThat(deployment.getSpec().getTemplate().getSpec().getContainers())
                  .hasSize(1)
                  .first()
                  .satisfies(c -> assertThat(c.getImage()).isEqualTo(INITIAL_IMAGE));
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
