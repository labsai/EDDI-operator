# EDDI Operator — Makefile
# Convenience targets for build, test, bundle, and image management

VERSION ?= 6.0.0
REGISTRY ?= quay.io
ORG ?= labsai
OPERATOR_IMG ?= $(REGISTRY)/$(ORG)/eddi-operator:$(VERSION)
BUNDLE_IMG ?= $(REGISTRY)/$(ORG)/eddi-operator-bundle:$(VERSION)
CATALOG_IMG ?= $(REGISTRY)/$(ORG)/eddi-operator-catalog:latest

.PHONY: help build test test-unit test-integration package native \
        image image-native bundle bundle-validate push catalog clean

help: ## Display this help
	@awk 'BEGIN {FS = ":.*##"; printf "\nUsage:\n  make \033[36m<target>\033[0m\n\nTargets:\n"} \
		/^[a-zA-Z_-]+:.*?##/ { printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2 }' $(MAKEFILE_LIST)

build: ## Compile the operator
	./mvnw compile -DskipTests

test: test-unit test-integration ## Run all tests

test-unit: ## Run unit tests
	./mvnw test -Dtest="ai.labs.eddi.operator.unit.*"

test-integration: ## Run integration tests
	./mvnw test -Dtest="ai.labs.eddi.operator.integration.*"

package: ## Package the operator (JVM)
	./mvnw package -DskipTests

native: ## Build native image
	./mvnw package -Pnative -DskipTests

image: package ## Build JVM operator container image
	docker build -f Dockerfile.jvm -t $(OPERATOR_IMG) .

image-native: native ## Build native operator container image
	docker build -f Dockerfile.native -t $(OPERATOR_IMG)-native .

bundle: ## Generate OLM bundle
	./mvnw package -DskipTests
	@echo "Bundle generated in target/bundle/"

bundle-validate: bundle ## Validate OLM bundle
	operator-sdk bundle validate ./target/bundle/

push: image ## Push operator image to registry
	docker push $(OPERATOR_IMG)

catalog: ## Build FBC catalog image
	opm render $(BUNDLE_IMG) -o yaml >> catalog/eddi-operator/catalog.yaml
	opm validate catalog/
	docker build -f catalog.Dockerfile -t $(CATALOG_IMG) .

clean: ## Clean build artifacts
	./mvnw clean

install-crd: ## Install CRD into cluster
	kubectl apply -f target/kubernetes/eddis.eddi.labs.ai-v1.yml

deploy: ## Deploy operator to cluster
	./mvnw package -DskipTests \
		-Dquarkus.kubernetes.deploy=true

undeploy: ## Remove operator from cluster
	kubectl delete -f target/kubernetes/kubernetes.yml
