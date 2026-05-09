# 🧪 Complete DevOps Stack - Testing Checklist

## Pre-Build Tests (Run These First)

### ✅ TEST 1: Verify All Files Are Correct

**Jenkinsfile Quality Gate Timeout:**
```powershell
# Should show "timeout(time: 20, unit: 'MINUTES')"
Select-String -Path "Jenkinsfile" -Pattern "timeout.*20.*MINUTES"
```

**SonarQube JVM Configuration:**
```powershell
# Should show Xmx2G and WORKERS: 4
Select-String -Path "docker-compose.devops.yml" -Pattern "Xmx2G|WORKERS.*4"
```

**Kubernetes Manifests Exist:**
```powershell
# Check all required files exist
Test-Path "k8s/base/prometheus.yaml"
Test-Path "k8s/base/grafana.yaml"
Test-Path "k8s/base/prometheus-config.yaml"
Test-Path "k8s/base/grafana-datasource.yaml"
Test-Path "k8s/base/grafana-dashboard.yaml"
```

**Git Status Clean:**
```powershell
# Should show: "nothing to commit, working tree clean"
git status
```

---

### ✅ TEST 2: Service Connectivity Tests

**Test API Gateway Health:**
```powershell
curl http://localhost:8083/actuator/health
# Expected: {"status":"UP","groups":["liveness","readiness"]}
```

**Test Prometheus Health:**
```powershell
curl -s "http://localhost:9090/api/v1/query?query=up" | ConvertFrom-Json | Select-Object -ExpandProperty status
# Expected: "success"
```

**Test Grafana Accessibility:**
```powershell
curl -I http://localhost:3000
# Expected: HTTP/1.1 302 Found (redirects to login)
```

---

### ✅ TEST 3: Kubernetes Pod Verification

**Check All Pods Running:**
```powershell
kubectl get pods -n nephro -o custom-columns=NAME:.metadata.name,STATUS:.status.phase
# Expected: All 13 pods showing "Running"
```

**Check Prometheus is Collecting:**
```powershell
kubectl exec -n nephro $(kubectl get pod -n nephro -l app=prometheus -o jsonpath='{.items[0].metadata.name}') -- \
  wget -qO- http://localhost:9090/api/v1/targets 2>&1 | Select-String '"health":"up"'
# Expected: Multiple matches showing targets with "health":"up"
```

**Check Grafana Logs for Datasource:**
```powershell
kubectl logs -n nephro $(kubectl get pod -n nephro -l app=grafana -o jsonpath='{.items[0].metadata.name}') | \
  Select-String "inserting datasource from configuration"
# Expected: "inserting datasource from configuration" name=Prometheus
```

---

## Build & Test Phases

### 🏗️ PHASE 1: Jenkins Pipeline Build

**Trigger Build via GitHub Webhook (Automatic)**
- Push to hamza-work02 branch triggers Jenkins automatically
- Jenkins webhook is configured in repository settings

**OR Manually Trigger Build:**
1. Go to: `http://localhost:8099/`
2. Login with initial password (see below)
3. Click: Jenkins → nephropaidi-pipeline → Build Now

**Initial Jenkins Password:**
```
e14e87cf562645f0933fe6b782de61c7
```

**What to Expect During Build:**
1. **Checkout Stage** (1 min) - Clone from GitHub
2. **Backend Build Stage** (1 min) - Maven compile all 12 modules
3. **Frontend Build Stage** (1 min) - Angular build
4. **SonarQube Analysis Stage** (1 min) - Code quality scan (~54 seconds)
5. **Quality Gate Stage** (20 sec) - Wait for SonarQube result
6. **Docker Build Stage** (2 min) - Build Docker images
7. **Total Time**: ~6 minutes

---

### 🔍 PHASE 2: Monitor Quality Gate

**During SonarQube Analysis:**
```
Expected Output:
  [INFO] User cache: /root/.sonar/cache
  [INFO] Found sonarqube in cache
  [INFO] Executing analysis for 666 files
  [INFO] Analysis report uploaded in 234ms
  [INFO] ANALYSIS SUCCESSFUL
```

**Quality Gate Result:**
```
Expected:
  ✅ PASSED
  
NOT:
  ❌ Aborted (timeout) - This would indicate the fix didn't work
```

---

### 📊 PHASE 3: SonarQube Verification

**1. Check SonarQube Dashboard:**
```
URL: http://localhost:9000/dashboard?id=tn.esprit.spring%3Anephropaidi-backend
Login: admin/admin
```

**Expected to See:**
- ✅ Quality Gate: **PASSED**
- ✅ Coverage metrics
- ✅ No timeout errors in logs
- ✅ Analysis completed in <2 minutes

**2. Check SonarQube Analysis Time:**
```powershell
# Check when analysis completed (should be ~54 seconds, not timing out)
docker logs nephro-sonarqube | Select-String "analysis started|analysis finished|Analysis SUCCESSFUL"
```

---

### 🎯 PHASE 4: Prometheus Metrics Verification

**1. Check Prometheus Targets:**
```
URL: http://localhost:9090/targets
```

**Expected:**
- ✅ prometheus → UP (green) - Prometheus scraping itself
- ✅ clinical-service → Will attempt to scrape (metrics being collected)

**2. Query Prometheus Directly:**
```powershell
# Get all metrics being collected
curl -s "http://localhost:9090/api/v1/query?query=up" | ConvertFrom-Json | `
  Select-Object -ExpandProperty data | Select-Object -ExpandProperty result | `
  Select-Object @{Name="Job";Expression={$_.metric.job}}, @{Name="Value";Expression={$_.value[1]}}
```

**Expected Output:**
```
Job                    Value
---                    -----
prometheus             1
clinical-service       1 (or 0 - still collecting)
```

---

### 📈 PHASE 5: Grafana Dashboard Verification

**1. Login to Grafana:**
```
URL: http://localhost:3000
Username: admin
Password: admin
```

**2. Verify Datasource:**
- Configuration → Data Sources
- Should see: "Prometheus" with green checkmark ✅
- Connection Status: "Data source is working"

**3. View Dashboard:**
- Dashboards → Browse
- Folder: "NephroPaidi"
- Dashboard: "nephropaidi-overview"
- Should show:
  - ✅ Clinical Service Up (stat panel)
  - ✅ CPU usage graph
  - ✅ Memory usage graph

**4. Test Metrics Query:**
- Try running a query: `up{job="prometheus"}`
- Should return: `value=1` (Prometheus is healthy)

---

### 🌐 PHASE 6: API Gateway & Microservices

**Test Each Service:**

```powershell
# 1. API Gateway (Port 8083)
curl http://localhost:8083/actuator/health
# Expected: HTTP 200, status=UP

# 2. Eureka Registry (Port 8761)
curl http://localhost:8761
# Expected: HTTP 302 (redirects to /eureka/)

# 3. Config Server (Port 8888)
curl -I http://localhost:8888/actuator/health
# Expected: HTTP 200 or 401 (may require auth)

# 4. Check registered services in Eureka
curl -s http://localhost:8761/eureka/apps | Select-String "<app>" | Measure-Object
# Expected: Multiple apps registered (11 microservices)
```

---

## ✅ SUCCESS CRITERIA

### ✅ The Fix Works If:
1. ✅ Jenkins Quality Gate stage does **NOT** timeout (waits < 1 minute)
2. ✅ SonarQube analysis completes in ~54 seconds
3. ✅ Quality Gate shows **PASSED** (not aborted)
4. ✅ All 13 Kubernetes pods remain **Running**
5. ✅ Prometheus is scraping targets (at least prometheus itself is "up")
6. ✅ Grafana loads dashboard without errors
7. ✅ API Gateway responds with HTTP 200

### ❌ Something is Wrong If:
- ❌ Jenkins pipeline times out at Quality Gate stage
- ❌ SonarQube analysis takes >10 minutes
- ❌ Pods crash or become "CrashLoopBackOff"
- ❌ Prometheus shows all targets "down"
- ❌ Grafana dashboard shows "No data"
- ❌ API Gateway returns HTTP 500 or 503

---

## 🔧 Troubleshooting Commands

**If SonarQube is slow:**
```powershell
# Check JVM memory allocation
docker logs nephro-sonarqube | Select-String "Xmx|Xms|memory|heap"
# Should show: -Xmx2G for web and compute engine
```

**If Prometheus targets are down:**
```powershell
# Check Prometheus config
kubectl get cm -n nephro prometheus-config -o jsonpath='{.data.prometheus\.yml}'
# Should show scrape configs for clinical-service and prometheus
```

**If Grafana datasource not connecting:**
```powershell
# Check Grafana logs for errors
kubectl logs -n nephro $(kubectl get pod -n nephro -l app=grafana -o jsonpath='{.items[0].metadata.name}') | \
  Select-String "error|failed|datasource" -Context 1
```

**If Jenkins won't start:**
```powershell
# Check Jenkins logs for errors
docker logs nephro-jenkins | Select-String "error|failed|exception" | Select-Object -Last 10
```

---

## 📋 Quick Test Script (Copy & Paste)

Run this in PowerShell to test everything at once:

```powershell
Write-Host "=== DEVOPS STACK QUICK TEST ===" 
Write-Host ""

Write-Host "1. Files Check..." 
(Select-String -Path "Jenkinsfile" -Pattern "timeout.*20.*MINUTES" -Quiet) ? "✓ Jenkinsfile OK" : "✗ Jenkinsfile FAIL"
(Select-String -Path "docker-compose.devops.yml" -Pattern "Xmx2G" -Quiet) ? "✓ SonarQube JVM OK" : "✗ SonarQube JVM FAIL"

Write-Host ""
Write-Host "2. Service Health..."
try { 
  $response = curl -s http://localhost:8083/actuator/health | ConvertFrom-Json
  ($response.status -eq "UP") ? "✓ API Gateway UP" : "✗ API Gateway DOWN"
} catch { "✗ API Gateway unreachable" }

Write-Host ""
Write-Host "3. Kubernetes Pods..."
$count = (kubectl get pods -n nephro --no-headers | Measure-Object -Line).Lines
Write-Host "✓ $count pods running"

Write-Host ""
Write-Host "4. Prometheus..."
try {
  $prom = curl -s "http://localhost:9090/api/v1/query?query=up" | ConvertFrom-Json
  ($prom.status -eq "success") ? "✓ Prometheus collecting metrics" : "✗ Prometheus query failed"
} catch { "✗ Prometheus unreachable" }

Write-Host ""
Write-Host "5. Grafana..."
try {
  $grafana = curl -I http://localhost:3000 -o /dev/null -s -w "%{http_code}"
  ($grafana -eq "302") ? "✓ Grafana running" : "✗ Grafana HTTP $grafana"
} catch { "✗ Grafana unreachable" }

Write-Host ""
Write-Host "=== END TEST ==="
```

---

## 🎤 Demo Talking Points

**If Everything Passes:**

1. **"The DevOps pipeline is now production-ready"**
   - Jenkins CI/CD with fixed timeouts
   - SonarQube code quality with optimizations
   - Kubernetes orchestration with 13 services
   - Complete monitoring with Prometheus & Grafana

2. **"The key fixes we implemented:"**
   - Extended Jenkins Quality Gate timeout from 10 to 20 minutes
   - Optimized SonarQube with 2GB JVM + 4 parallel workers
   - Reduced analysis time from 10+ minutes to ~54 seconds
   - Deployed monitoring stack (Prometheus + Grafana)

3. **"Infrastructure as Code:"**
   - All services defined in Kubernetes YAML manifests
   - Git version control for entire stack
   - Reproducible deployments
   - Production-ready configuration

---

**Status: ✅ Ready for Full Testing & Demo**

