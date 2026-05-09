# DevOps Recap - NephroPaidi

## 1) Global DevOps Plan (General)
1. CI/CD automation with Jenkins + GitHub Webhooks (Front + Back).
2. Code quality analysis with SonarQube + Quality Gate in Jenkins pipeline.
3. Docker image build and push (Front + Back) to a registry.
4. Kubernetes orchestration on a kubeadm cluster for application services.
5. Monitoring and observability with Prometheus + Grafana.
6. Final delivery evidence (screenshots, logs, successful pipeline runs).

## 2) What We Already Completed
1. Jenkins installed and configured.
2. Jenkins pipeline created and running from GitHub branch.
3. GitHub PAT credentials configured in Jenkins.
4. SonarQube integrated with Jenkins pipeline.
5. Quality Gate flow validated in pipeline (including webhook handling).
6. Dockerized DevOps stack prepared:
   - `docker-compose.devops.yml`
   - `docs/DEVOPS_PHASE_2_DOCKERIZED_CI_STACK.md`
7. Kubernetes base manifests created and committed for:
   - core: namespace, ingress, api-gateway, config-server, eureka, keycloak, rabbitmq
   - services batch 1: user-service, administration-service, communication-service
   - services batch 2: procedure-service, ops-service, pharmacy-service
   - additional services: clinical-service, ai-clinical-service, egfr-ml-service, ai-pharmacy-service, core-ops-service, frontend
8. Health check through ingress succeeded at different points:
   - `curl http://nephro.local/actuator/health` => `{"status":"UP"...}`
9. DevOps/K8s work pushed to branch `hamza-work02`.
10. Runtime generated files ignored:
   - added `BackEnd/runtime/clinical-lab-results/` to `.gitignore`.

## 3) What Is Still Missing
1. Replace the local Minikube/Docker Desktop validation path with a kubeadm-specific deployment and test route.
2. Stabilize registry-based image pull/runtime readiness on the target cluster.
3. Complete Jenkins Docker image push proof for the jury path.
4. Validate Prometheus/Grafana from the target kubeadm cluster.
5. Optional hardening:
   - move sensitive env values to Kubernetes Secrets
   - split manifests into base + overlays (dev/prod)
6. Prepare final evaluation proof package (captures + logs per rubric line).

## 4) Current Position (Where We Stopped)
- **Current phase:** Phase 3 - Kubernetes Orchestration (kubeadm target).
- **Current step:** CI, SonarQube, registry path, manifests, and monitoring are partially in place; the jury route now needs kubeadm-specific deployment validation.
- **Next immediate step:** prepare kubeadm cluster prerequisites, publish images to a registry, then apply and validate manifests there.

## 5) Quick Next Session Checklist
1. Bring up the kubeadm cluster and install an ingress controller.
2. Push the required images to a registry reachable by the kubeadm nodes.
3. Apply K8s manifests:
   - `kubectl apply -k k8s/base`
4. Verify pods/services/ingress:
   - `kubectl get pods -n nephro`
   - `kubectl get svc -n nephro`
   - `kubectl get ingress -n nephro`
5. Validate:
   - `http://nephro.local/actuator/health`
   - `http://front.nephro.local`
   - `http://prometheus.nephro.local`
   - `http://grafana.nephro.local`
