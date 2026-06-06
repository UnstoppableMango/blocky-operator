package dev.unmango;

public class BlockySpec {

  private String image = "ghcr.io/0xERR0R/blocky:latest";
  private BlockyConfig config = new BlockyConfig();

  public String getImage() {
    return image;
  }

  public void setImage(String image) {
    this.image = image;
  }

  public BlockyConfig getConfig() {
    return config;
  }

  public void setConfig(BlockyConfig config) {
    this.config = config;
  }
}
