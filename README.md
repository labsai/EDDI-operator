# eddi-operator

[![Docker Repository on Quay](https://quay.io/repository/labsai/eddi-operator/status "Docker Repository on Quay")](https://quay.io/repository/labsai/eddi-operator)

## Usage

Deploy an EDDI instance by creating a new EDDI resource in the desired Namespace.

```Yaml
apiVersion: labs.ai/v1alpha1
kind: Eddi
metadata:
  name: eddi
spec:
  size: 1
```

The operator will deploy a MongoDB pod, an EDDI Instance and create a route to access the EDDI WebUI. The route is automatically created.

Example route based on the resource above deployed to a namespace called test: `eddi-route-test.apps.ocp.example.com`

Additionally the storage class used for the MongoDB deployment can be set in the CustomResource:

```yaml
apiVersion: labs.ai/v1alpha1
kind: Eddi
metadata:
  name: eddi
spec:
  size: 1
  mongodb:
    environment: prod
    storageclass_name: managed-nfs-storage
    storage_size: 20G
```
