# DevOps Recap - NephroPaidi

## 1) Global DevOps Plan (General)
1. CI/CD automation with Jenkins + GitHub Webhooks (Front + Back).
2. Code quality analysis with SonarQube + Quality Gate in Jenkins pipeline.
3. Docker image build and push (Front + Back) to a registry.
4. Kubernetes orchestration (Minikube) for application services.
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
7. Minikube installed and working with Docker driver + containerd runtime.
8. Kubernetes base manifests created and committed for:
   - core: namespace, ingress, api-gateway, config-server, eureka, keycloak, rabbitmq
   - services batch 1: user-service, administration-service, communication-service
   - services batch 2: procedure-service, ops-service, pharmacy-service
9. Health check through ingress succeeded at different points:
   - `curl http://nephro.local/actuator/health` => `{"status":"UP"...}`
10. DevOps/K8s work pushed to branch `hamza-work02` (commit `80c85a25`).
11. Runtime generated files ignored:
   - added `BackEnd/runtime/clinical-lab-results/` to `.gitignore`.

## 3) What Is Still Missing
1. Finish Kubernetes deployment for remaining services:
   - `clinical-service`
   - `ai-clinical-service`
   - `egfr-ml-service`
   - `ai-pharmacy-service`
   - `core-ops-service`
   - frontend
2. Stabilize image pull/runtime readiness when cluster is under load (mainly RabbitMQ/Keycloak pull delays).
3. Add Docker image push stage(s) in Jenkins pipeline (for professor grading criterion).
4. Implement Monitoring phase:
   - Prometheus deployment/config
   - Grafana deployment/config
   - dashboards and service targets
5. Optional hardening:
   - move sensitive env values to Kubernetes Secrets
   - split manifests into base + overlays (dev/prod)
6. Prepare final evaluation proof package (captures + logs per rubric line).

## 4) Current Position (Where We Stopped)
- **Current phase:** Phase 3 - Kubernetes Orchestration (Minikube).
- **Current step:** Batch 2 services added and committed; cluster reset/redeploy performed; validating stable runtime for all pods before moving to remaining services.
- **Next immediate step:** Deploy remaining services in K8s, then start Phase 4 (Prometheus + Grafana).

## 5) Quick Next Session Checklist
1. Start Docker Desktop and confirm Engine running.
2. Start Minikube:
   - `minikube start --driver=docker --container-runtime=containerd --cpus=2 --memory=4096`
3. Apply K8s manifests:
   - `minikube kubectl -- apply -k k8s/base`
4. Verify pods/services:
   - `minikube kubectl -- get pods -n nephro`
   - `minikube kubectl -- get svc -n nephro`
5. Keep tunnel open in separate terminal:
   - `minikube tunnel`
6. Validate ingress:
   - `curl.exe http://nephro.local/actuator/health`
7. Continue with remaining services manifests/deployments.
