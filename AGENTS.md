<claude-mem-context>
# Memory Context

# [Esprit-PI-Sae9-2026-NephroPaidi] recent context, 2026-05-08 10:18am GMT+1

Legend: 🎯session 🔴bugfix 🟣feature 🔄refactor ✅change 🔵discovery ⚖️decision 🚨security_alert 🔐security_note
Format: ID TIME TYPE TITLE
Fetch details: get_observations([IDs]) | Search: mem-search skill

Stats: 50 obs (16 288t read) | 87 685t work | 81% savings

### May 7, 2026
S2 Minikube start failure on Windows 11 — diagnosing root cause and planning disk space remediation (May 7, 9:29 PM)
S3 Minikube start failure on Windows 11 — root cause identified as disk full, planning safe remediation (May 7, 9:29 PM)
474 10:23p 🟣 Nginx Ingress Controller v1.15.1 Installed on Docker Desktop Kubernetes
475 " 🔵 Nginx Ingress Controller Pod Failed to Reach Ready State Within 3 Minutes
476 " 🔵 Nginx Ingress Controller LoadBalancer Assigned localhost as External IP
477 10:24p 🔵 All Ingress-Nginx Pods Still in ContainerCreating — Image Pull in Progress
478 " 🔵 Nginx Ingress Controller Pod Running But Not Yet Ready
479 " 🔵 Ingress Controller Blocked by Missing Webhook Secret and Slow Image Pull
480 " 🔵 Admission-Create Job Pod Already Cleaned Up — Job Completed Successfully
481 " 🟣 Nginx Ingress Controller Fully Operational — 1/1 Running
482 10:25p 🔵 Ingress Controller Full Startup Timeline and Image Sizes Documented
483 " 🟣 Frontend Service Reachable via Nginx Ingress on localhost
484 " 🟣 All NephroPaidi Ingress Endpoints Verified Healthy via HTTP
485 " 🔵 API Gateway Actuator Health Endpoint Returns UP via Ingress
486 " 🔵 NephroPaidi Hostnames Not Yet in Windows Hosts File
487 " 🔵 Hosts File Write Blocked by Windows UAC — Requires Elevated Privileges
488 " 🔵 scripts/rebuild-redeploy.ps1 Exists and Has Valid PowerShell Syntax
489 10:26p ✅ Project Pivoted from Minikube to Kubeadm as Jury Delivery Target
490 10:30p 🔵 pdftotext not installed on Windows environment
491 10:31p 🔵 NephroPaidi DevOps Sprint 3 infrastructure state mapped
492 " 🔵 Grafana 11.1.0 confirmed live and healthy on localhost
493 " 🔵 Python cp1252 encoding blocks PDF text extraction on Windows
494 " 🔵 Prometheus live target audit: clinical-service DOWN with HTTP 404 on /actuator/prometheus
495 " 🔵 Sprint 3 DevOps evaluation rubric fully extracted
496 " 🔵 clinical-service actuator config present but Prometheus still gets 404
497 10:32p 🔵 clinical-service application.yml complete but missing explicit prometheus endpoint enable and config server may override
498 " 🔵 clinical-service missing micrometer-registry-prometheus dependency
499 " 🔵 clinical-service pom.xml confirmed missing micrometer-registry-prometheus; Spring Boot 3.5.10 + Spring Cloud 2025.0.1
500 " 🔵 clinical-service pod confirmed: /actuator/health UP but /actuator/prometheus 404 from inside pod
501 " 🔴 Added micrometer-registry-prometheus to clinical-service pom.xml
502 " 🔵 clinical-service Docker build fails: Dockerfile.ci not found at BackEnd/microservices/clinical-service/Dockerfile.ci
503 10:33p 🔵 clinical-service has only a plain Dockerfile, no Dockerfile.ci
504 " 🔵 clinical-service docker-compose build context confirmed; Neon.tech DB credentials exposed in plain text
505 " 🔐 Neon.tech database password hardcoded in docker-compose.full.yml
506 " 🔵 Backend Maven aggregator POM lists 11 microservice modules
507 10:37p 🔵 Minikube Startup Fails Due to Windows File Permission Errors
508 " 🟣 clinical-service Docker Image Built Successfully
509 10:38p 🚨 Plaintext Database Credentials in Kubernetes Manifest
510 " 🔵 clinical-service Kubernetes Manifest Configuration
511 " ✅ clinical-service Redeployed to Kubernetes Successfully
512 " 🔵 clinical-service Running on Docker Desktop Kubernetes, Not Minikube
513 " 🔵 Pod Replaced After Rollout Restart — Old Pod Name No Longer Valid
514 " 🔵 clinical-service Actuator Prometheus Endpoint Returns Empty Response
515 " 🔵 Prometheus Cannot Scrape clinical-service — Connection Refused on Port 8084
516 10:39p 🔵 clinical-service Rollout Complete — New ReplicaSet Active, Prometheus Still Cannot Reach Service
517 " 🔵 clinical-service Returns HTTP 404 on /actuator/prometheus — Endpoint Not Exposed
518 10:41p 🔵 Prometheus Scrape Error Updated from "Connection Refused" to HTTP 404
519 " 🔵 Grafana Accessible via Ingress with Default admin/admin Credentials
520 " 🔵 clinical-service SecurityConfig Only Permits /actuator/health and /actuator/info — Not /actuator/prometheus
521 " 🔵 SecurityConfig Active Only on Non-local Profile — clinical-service Runs with Profile "local"
522 " 🔵 Config Server clinical-service.yml Confirms Prometheus Endpoint Not Exposed
523 " 🔴 Prometheus Actuator Endpoint Exposed in clinical-service Config

Access 88k tokens of past work via get_observations([IDs]) or mem-search skill.
</claude-mem-context>