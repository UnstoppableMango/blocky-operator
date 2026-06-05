KIND_CLUSTER_NAME ?= blocky-operator
KIND_KUBECONFIG   := .kind/kubeconfig

.PHONY: build update check lint format fmt test start-kind stop-kind

build:
	nix build .#

update:
	nix flake update

check lint:
	nix flake check

format fmt:
	nix fmt

test:
	mvn test

$(KIND_KUBECONFIG): kind-config.yaml
	mkdir -p .kind
	kind create cluster \
	  --name $(KIND_CLUSTER_NAME) \
	  --config kind-config.yaml \
	  --kubeconfig $(KIND_KUBECONFIG)

start-kind: $(KIND_KUBECONFIG)

stop-kind:
	kind delete cluster --name $(KIND_CLUSTER_NAME)
	rm -f $(KIND_KUBECONFIG)
