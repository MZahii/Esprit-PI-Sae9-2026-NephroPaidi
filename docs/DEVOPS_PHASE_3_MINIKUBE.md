# DevOps Phase 3 - Minikube + Kubernetes

This phase deploys a first Kubernetes slice of the platform on Minikube:
- `keycloak`
- `eureka`
- `config-server`
- `api-gateway`

All resources are in namespace `nephro`.

## 1) Prerequisites

- Docker Desktop running
- Minikube running

Check:

```powershell
minikube status
minikube kubectl -- get nodes
```

## 2) Build app images locally

From repo root:

```powershell
docker compose -f docker-compose.full.yml build eureka config-server api-gateway
```

## 3) Load images into Minikube

```powershell
minikube image load nephropaidi-eureka:latest
minikube image load nephropaidi-config-server:latest
minikube image load nephropaidi-api-gateway:latest
```

## 4) Apply Kubernetes manifests

```powershell
minikube kubectl -- apply -k k8s/base
minikube kubectl -- get pods -n nephro
minikube kubectl -- get svc -n nephro
```

## 5) Access through Ingress

Ingress host configured: `nephro.local`

Run tunnel in a dedicated terminal:

```powershell
minikube tunnel
```

Then add to Windows hosts file:

```text
127.0.0.1 nephro.local
```

Test gateway:

```powershell
curl http://nephro.local/actuator/health
```

## Notes

- This is a starter Kubernetes slice for Phase 3 validation.
- Remaining microservices can be added incrementally in the same `k8s/` structure.

