{
  jre,
  lib,
  makeWrapper,
  maven,
}:
let
  version = "0.1.0";
  jarFile = "blocky-operator-${version}-SNAPSHOT.jar";
in
maven.buildMavenPackage {
  pname = "blocky-operator";
  inherit version;

  src = lib.cleanSource ../.;
  mvnHash = "sha256-a2XYiaanHX67/QGC7Li7tQM7HbZ38x/1ntu0e525ISI=";

  nativeBuildInputs = [ makeWrapper ];

  doCheck = false;

  installPhase = ''
    mkdir -p $out/bin $out/share/blocky-operator
    install -Dm644 target/${jarFile} $out/share/blocky-operator

    makeWrapper ${jre}/bin/java $out/bin/blocky-operator \
      --add-flags "-jar $out/share/blocky-operator/${jarFile}"
  '';
}
