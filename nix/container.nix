{ pkgs, n2c, operator }:

n2c.buildImage {
  name = "ghcr.io/unstoppablemango/blocky-operator";

  layers = [
    (n2c.buildLayer { deps = [ pkgs.jre pkgs.cacert ]; })
    (n2c.buildLayer { deps = [ operator ]; })
  ];

  config = {
    Cmd = [ "${operator}/bin/blocky-operator" ];
    User = "65534:65534";
    Env = [
      "SSL_CERT_FILE=${pkgs.cacert}/etc/ssl/certs/ca-bundle.crt"
    ];
  };
}
