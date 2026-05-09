# DevOps Runbook - Webhook, GHCR, and kubeadm

This is the shortest practical route to complete the remaining Sprint 3 items:

1. Jenkins auto-trigger from GitHub push
2. Docker image push to GHCR
3. kubeadm deployment from registry images

## 1) Jenkins prerequisites

The Jenkins container must run from the custom image with Docker CLI:

- service: `jenkins`
- compose file: `docker-compose.devops.yml`
- image: `nephropaidi-jenkins-docker:lts`

Verify inside Jenkins container:

```bash
docker version
```

## 2) Required Jenkins credentials

Create these Jenkins credentials:

- `github-pat-userpass`
  - Type: `Username with password`
  - Username: your GitHub username
  - Password: GitHub PAT
- `sonarqube-token`
  - Type: `Secret text`
  - Value: SonarQube user token

## 3) Jenkins job parameters for Docker push

In `nephropaidi-pipeline`, run with:

- `RUN_FRONTEND_TESTS=false`
- `RUN_SONAR=true`
- `BUILD_DOCKER_IMAGES=true`
- `PUSH_DOCKER_IMAGES=true`
- `DOCKER_NAMESPACE=mzahii`
- `IMAGE_TAG=latest`

Expected pushed images include at least:

- `ghcr.io/mzahii/nephropaidi-api-gateway:latest`
- `ghcr.io/mzahii/nephropaidi-frontend:latest`

For full kubeadm deployment, publish the remaining app images as well.

## 4) GitHub webhook setup

GitHub cannot call `localhost`, so Jenkins needs a public URL or tunnel.

Use a tunnel such as ngrok:

```bash
ngrok http 8099
```

Then configure the GitHub repository webhook:

- Payload URL: `https://<public-url>/github-webhook/`
- Content type: `application/json`
- Events: `Just the push event`

In the Jenkins pipeline job, enable:

- `GitHub hook trigger for GITScm polling`

Proof expected for the jury:

- one Git push
- one automatically triggered Jenkins build
- successful console output / stage view

## 5) GHCR pull secret for kubeadm

Create the image pull secret on the kubeadm cluster before deploying the overlay:

```bash
kubectl create namespace nephro

kubectl create secret docker-registry ghcr-pull-secret \
  --namespace nephro \
  --docker-server=ghcr.io \
  --docker-username=<github-username> \
  --docker-password=<github-pat> \
  --docker-email=<email>
```

## 6) kubeadm deployment path

Use the overlay:

```bash
kubectl apply -k k8s/overlays/kubeadm
kubectl get pods -n nephro
kubectl get ingress -n nephro
```

The overlay:

- rewrites local images to `ghcr.io/mzahii/...`
- injects `imagePullSecrets: [ghcr-pull-secret]`

## 7) Hosts for browser validation

Map these names to the ingress entrypoint:

```text
<ingress-ip> nephro.local
<ingress-ip> front.nephro.local
<ingress-ip> grafana.nephro.local
<ingress-ip> prometheus.nephro.local
```

## 8) Monitoring proof

Expected browser routes:

- `http://grafana.nephro.local`
- `http://prometheus.nephro.local`
- `http://front.nephro.local`
- `http://nephro.local/actuator/health`

Grafana proof should show:

- dashboard `NephroPaidi Monitoring`
- Prometheus datasource healthy
- `clinical-service` target `up`
- `prometheus` target `up`
