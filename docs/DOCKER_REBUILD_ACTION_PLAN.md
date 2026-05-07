# Docker Rebuild & Kubernetes Redeploy Action Plan

**Status:** Infrastructure crashed (Docker Desktop + Kubernetes)  
**Reason:** Metrics configuration in source files but NOT in deployed Docker images  
**Solution:** Complete rebuild cycle

---

## 🔴 What Happened

The application configuration files HAVE the Prometheus metrics setup:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

**BUT** the Docker images were built BEFORE these configuration changes and deployed to Kubernetes.

**Result:** 
- ✅ Config files correct
- ✅ K8s pods running
- ❌ Services NOT exposing `/actuator/prometheus` endpoint
- ❌ Prometheus sees only 2 targets (prometheus itself + clinical-service)

---

## 🟡 What Needs to Happen

### STEP 1: Restart Infrastructure
**When Docker/K8s come back online:**

```powershell
# Restart Docker Desktop manually or:
Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe"

# Wait 2-3 minutes for it to fully start
# Then verify:
docker ps
docker ps --format "table {{.Names}}\t{{.Status}}"
```

### STEP 2: Run the Rebuild Script
Once infrastructure is back up:

```powershell
cd "C:\Users\zehim\OneDrive - ESPRIT\Bureau\PI\Esprit-PI-Sae9-2026-NephroPaidi"
.\scripts\rebuild-redeploy.ps1
```

**This script does:**
1. ✅ Rebuild all 15 Docker images with `--no-cache` (forces fresh build)
2. ✅ Load all 11 microservice images into Minikube
3. ✅ Restart all 11 Kubernetes deployments
4. ✅ Wait 45 seconds for pods to stabilize
5. ✅ Verify all pods are Running
6. ✅ Test metrics endpoints on 3 services
7. ✅ Query Prometheus for target status
8. ✅ Display summary report

**Estimated time:** 15-20 minutes total

---

## 🟢 Expected Results After Rebuild

### Prometheus Targets (http://localhost:9090/targets)
Should show all as **UP** (green):
```
✅ prometheus         : up
✅ clinical-service   : up
✅ api-gateway        : up
✅ user-service       : up
✅ pharmacy-service   : up
✅ communication-service : up
✅ administration-service: up
✅ config-server      : up
✅ eureka             : up
... (all 11-13 showing UP)
```

### Grafana Dashboard (http://localhost:3000)
- Navigate: **Home → NephroPaidi → nephropaidi-overview**
- Should see real-time metrics:
  - CPU Usage graph
  - Memory Usage graph
  - HTTP Request metrics
  - JVM metrics

### Jenkins Pipeline (http://localhost:8099)
- Build time: 2-3 minutes (vs 40+ before)
- Quality Gate: UNSTABLE but build succeeds
- All stages complete successfully

---

## 📋 Quick Checklist

After script completes, verify:

```powershell
# 1. Count running pods (should be 13+)
kubectl get pods -n nephro --no-headers | Measure-Object -Line

# 2. Check pod images contain new build
kubectl get pods -n nephro -o jsonpath='{.items[0].spec.containers[0].image}'

# 3. Test a metrics endpoint directly
curl http://localhost:8084/actuator/prometheus | Select-Object -First 10

# 4. Check Prometheus targets
curl -s "http://localhost:9090/api/v1/targets" | ConvertFrom-Json | Select-Object -ExpandProperty data | Select-Object -ExpandProperty activeTargets | ForEach-Object { "$($_.labels.job): $($_.health)" }

# 5. Verify Grafana datasource
curl -s "http://localhost:3000/api/datasources" | ConvertFrom-Json
```

---

## 🆘 Troubleshooting

### If Prometheus targets still DOWN:

1. **Check pod logs:**
```powershell
kubectl logs -n nephro clinical-service-xxxxx | grep -i "actuator\|prometheus\|management"
```

2. **Verify pod is actually running:**
```powershell
kubectl exec -it -n nephro clinical-service-xxxxx -- curl localhost:8084/actuator/health
```

3. **Check if image was loaded:**
```powershell
docker images | grep nephropaidi-clinical-service
minikube image ls | grep nephropaidi-clinical-service
```

### If Docker won't restart:

Try nuclear option:
```powershell
# Reset Docker Desktop
Settings → Troubleshoot → Reset/Restart Engine
```

### If Kubernetes won't connect:

```powershell
# Reset Minikube
minikube delete
minikube start --cpus 4 --memory 8192 --driver hyperv

# Then redeploy:
kubectl apply -f k8s/base/
```

---

## 📍 Key Files

- **Rebuild Script:** `scripts/rebuild-redeploy.ps1` (newly created)
- **Source Configs:** `BackEnd/microservices/*/src/main/resources/application.yml`
- **Docker Compose:** `docker-compose.full.yml`
- **K8s Manifests:** `k8s/base/`
- **Latest Commit:** a51f001b (rebuild script added)

---

## 🎯 Success Criteria

✅ **Complete** when:

1. All 13+ pods show `1/1 Running`
2. Prometheus `/api/v1/targets` shows all microservices as `up`
3. Grafana `nephropaidi-overview` dashboard displays metrics
4. Services respond to `/actuator/prometheus` with HTTP 200
5. Jenkins builds complete in 2-3 minutes

---

## ✨ What This Fixes

- ✅ Prometheus can now scrape metrics from all services
- ✅ Grafana dashboards can display real data
- ✅ Complete observability stack operational
- ✅ All microservices instrumented for monitoring
- ✅ Production-ready monitoring infrastructure

---

**Status:** Ready to deploy  
**Next Action:** Wait for Docker/K8s to stabilize → Run `rebuild-redeploy.ps1`
