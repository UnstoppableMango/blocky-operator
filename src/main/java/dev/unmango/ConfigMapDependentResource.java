package dev.unmango;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import java.util.Map;

@KubernetesDependent
public class ConfigMapDependentResource
    extends CRUDKubernetesDependentResource<ConfigMap, Blocky> {

  public static final String CONFIG_VOLUME_NAME = "blocky-config";
  public static final String CONFIG_MOUNT_PATH = "/app/config.yml";
  public static final String CONFIG_KEY = "config.yml";

  private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

  @Override
  protected ConfigMap desired(Blocky primary, Context<Blocky> context) {
    return new ConfigMapBuilder()
        .withMetadata(
            new ObjectMetaBuilder()
                .withName(configMapName(primary))
                .withNamespace(primary.getMetadata().getNamespace())
                .build())
        .withData(Map.of(CONFIG_KEY, resolveConfigYaml(primary)))
        .build();
  }

  public static String configMapName(Blocky primary) {
    return primary.getMetadata().getName() + "-config";
  }

  private static String resolveConfigYaml(Blocky primary) {
    var config = primary.getSpec().getConfig();
    if (config == null) return "";
    if (config.getYaml() != null) return config.getYaml();
    if (config.getInline() != null) {
      try {
        return YAML_MAPPER.writeValueAsString(config.getInline());
      } catch (JsonProcessingException e) {
        throw new IllegalArgumentException("Failed to serialize inline config", e);
      }
    }
    return "";
  }
}
