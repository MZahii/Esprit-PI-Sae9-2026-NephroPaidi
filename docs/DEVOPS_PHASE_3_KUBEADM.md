# DevOps Phase 3 - kubeadm Cluster Route

This is the Sprint 3 jury path for orchestration. The rubric expects a **functional kubeadm cluster**.

All resources are under namespace `nephro`.

## 1) Cluster prerequisites

- Linux-based kubeadm cluster already initialized
- `kubectl` access from the admin machine
- An ingress controller installed, typically `ingress-nginx`
- A registry reachable by every cluster node

Check:

```bash
kubectl get nodes
kubectl get pods -A
kubectl get ingressclass
```

## 2) Build and publish the required images

At minimum for the jury flow, publish:

- `nephropaidi-api-gateway`
- `nephropaidi-frontend`

For full stack validation, also publish the remaining service images referenced in `k8s/base/`.

Recommended pattern:

```bash
docker build -t ghcr.io/<owner>/nephropaidi-api-gateway:<tag> -f BackEnd/api-gateway/Dockerfile.ci BackEnd/api-gateway
docker build -t ghcr.io/<owner>/nephropaidi-frontend:<tag> -f FrontEnd/Dockerfile.ci FrontEnd
docker push ghcr.io/<owner>/nephropaidi-api-gateway:<tag>
docker push ghcr.io/<owner>/nephropaidi-frontend:<tag>
```

If nodes cannot see local images, kubeadm deployment will not work until images are pushed to a shared registry.

## 3) Apply Kubernetes manifests

```bash
kubectl apply -k k8s/base
kubectl get pods -n nephro
kubectl get svc -n nephro
kubectl get ingress -n nephro
```

## 4) Hostnames and ingress

Expected ingress hosts:

- `nephro.local`
- `front.nephro.local`
- `prometheus.nephro.local`
- `grafana.nephro.local`

Map these hostnames to the ingress/controller IP in your local `hosts` file or DNS.

## 5) Validate the jury path

Backend:

```bash
curl http://nephro.local/actuator/health
```

Frontend:

```bash
curl http://front.nephro.local
```

Monitoring:

- `http://prometheus.nephro.local`
- `http://grafana.nephro.local`

## 6) Important cleanup items before jury

- Replace local-dev `localhost` Keycloak issuer URIs in K8s manifests with cluster-reachable URLs
- Move plain-text secrets into Kubernetes `Secret` objects
- Ensure webhook, SonarQube, and Docker push proof are captured
- Validate the frontend route from the kubeadm cluster, not only from local Docker/Desktop experiments
