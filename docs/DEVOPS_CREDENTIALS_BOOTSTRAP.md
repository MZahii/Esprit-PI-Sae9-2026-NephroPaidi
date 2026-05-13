# DevOps Credentials Bootstrap

This document is a safe handoff for teammates after they pull the repository.
It lists the devops-related credentials and where they are wired in the stack,
without copying secrets into a shareable document.

## What to use

| Service | Port / URL | Username | Password / Notes | Source |
| --- | --- | --- | --- | --- |
| Jenkins | `http://localhost:8099` | `jury-admin` | `NephroDevops2026!` from [devops/jenkins/init.groovy.d/10-create-demo-admin.groovy](../devops/jenkins/init.groovy.d/10-create-demo-admin.groovy) | [docker-compose.devops.yml](../docker-compose.devops.yml) |
| SonarQube | `http://localhost:9000` | `admin` | `admin` on first login; change it after sign-in | [docker-compose.devops.yml](../docker-compose.devops.yml) |
| Grafana | `http://localhost:3000` | `admin` | `admin` | [DEVOPS_FINAL_HANDOFF.md](../DEVOPS_FINAL_HANDOFF.md) |
| Prometheus | `http://localhost:9090` | none | no username/password by default; `/targets` is the main UI entry point | [DEVOPS_FINAL_HANDOFF.md](../DEVOPS_FINAL_HANDOFF.md) |

## Team bootstrap steps

1. Copy [.env.example](../.env.example) to `.env` at the repo root.
2. Copy [BackEnd/.env.example](../BackEnd/.env.example) to `BackEnd/.env`.
3. Fill the devops variables above from your shared secret source or local bootstrap standard.
4. Start the desired stack:
   - `docker compose -f docker-compose.devops.yml up -d` for Jenkins and SonarQube
   - `docker compose -f docker-compose.full.yml up -d` for the full platform
5. If you are using Kubernetes, update [k8s/base/nephro-app-secrets.yaml](../k8s/base/nephro-app-secrets.yaml) before applying overlays.

## Notes

- Jenkins uses a demo admin account created by the Groovy init script.
- SonarQube and Grafana both expose default admin/admin logins in this repo.
- Prometheus has no login unless you add one in front of it.
- The repository currently contains default values for some local-dev credentials in compose and K8s files; rotate them before external sharing.

Jenkins: http://localhost:8099, login jury-admin, password NephroDevops2026!
SonarQube: http://localhost:9000, login admin, password admin on first login
Grafana: http://localhost:3000, login admin, password admin
Prometheus: http://localhost:9090, no username/password by default