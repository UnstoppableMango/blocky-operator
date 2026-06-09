package dev.unmango;

import io.fabric8.crd.generator.annotation.PreserveUnknownFields;
import io.fabric8.kubernetes.api.model.LocalObjectReference;

public class BlockyConfig {

  private LocalObjectReference configMapRef;
  private String yaml;
  @PreserveUnknownFields
  private Object inline;

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

  public Object getInline() {
    return inline;
  }

  public void setInline(Object inline) {
    this.inline = inline;
  }
}
