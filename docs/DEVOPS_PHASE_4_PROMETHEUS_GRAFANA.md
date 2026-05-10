# DevOps Phase 4 - Prometheus + Grafana

This phase adds a first monitoring stack inside the `nephro` namespace:
- `prometheus`
- `grafana`

Current metric coverage:
- `prometheus` self-monitoring
- `clinical-service` via `/actuator/prometheus`

## 1) Apply the monitoring stack

From repo root:

```powershell
kubectl apply -k k8s/base
kubectl get pods -n nephro
kubectl get svc -n nephro
kubectl get ingress -n nephro
```

## 2) Add ingress hosts

Add these entries to your Windows hosts file:

```text
127.0.0.1 prometheus.nephro.local
127.0.0.1 grafana.nephro.local
```

If you are still using the existing gateway and frontend hosts, keep those too.

## 3) Access URLs

- Prometheus: `http://prometheus.nephro.local`
- Grafana: `http://grafana.nephro.local`

Grafana default login:
- user: `admin`
- password: `admin`

Change the password after the first login.

## 4) What to verify

- Prometheus target `prometheus` is `UP`
- Prometheus target `clinical-service` is `UP` once `clinical-service` is deployed
- Grafana datasource `Prometheus` is healthy
- Dashboard `NephroPaidi Monitoring` appears automatically

## 5) Important note

At the moment, only `clinical-service` is configured with a Prometheus metrics endpoint in the application stack.
Other services still need explicit Prometheus instrumentation or compatible exporters to appear with useful metrics in Grafana.
