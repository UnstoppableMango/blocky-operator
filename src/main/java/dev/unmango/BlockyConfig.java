package dev.unmango;

import io.fabric8.kubernetes.api.model.LocalObjectReference;
import java.util.Map;

public class BlockyConfig {

  private LocalObjectReference configMapRef;
  private String yaml;
  private Map<String, Object> inline;

  public LocalObjectReference getConfigMapRef() {
    return configMapRef;
  }

  public void setConfigMapRef(LocalObjectReference configMapRef) {
    this.configMapRef = configMapRef;
  }

  public String getYaml() {
    return yaml;
  }

  public void setYaml(String yaml) {
    this.yaml = yaml;
  }

  public Map<String, Object> getInline() {
    return inline;
  }

  public void setInline(Map<String, Object> inline) {
    this.inline = inline;
  }
}
