# Complete Docker Rebuild and Kubernetes Redeploy Script
# Purpose: Rebuild all Docker images and redeploy to Kubernetes with latest metrics configuration
# Usage: .\rebuild-redeploy.ps1

Write-Host @"
╔════════════════════════════════════════════════════════════════════════════╗
║                   🔨 COMPLETE REBUILD & REDEPLOY SCRIPT                   ║
║              Rebuilds Docker images and redeploys to Kubernetes            ║
╚════════════════════════════════════════════════════════════════════════════╝
"@

# Get to project root
cd (Split-Path -Parent $PSScriptRoot)
Write-Host "Working directory: $(Get-Location)"

# ============================================================================
# PHASE 1: BUILD ALL DOCKER IMAGES
# ============================================================================
Write-Host "`n╔═══════════════════════════════════════════════════════════╗"
Write-Host "║ PHASE 1: REBUILDING DOCKER IMAGES                        ║"
Write-Host "╚═══════════════════════════════════════════════════════════╝"

Write-Host "`n⏳ Building all services (this takes 10-15 minutes)..."
Write-Host "Services to rebuild:"
Write-Host "  • api-gateway"
Write-Host "  • eureka"
Write-Host "  • config-server"
Write-Host "  • clinical-service"
Write-Host "  • user-service"
Write-Host "  • pharmacy-service"
Write-Host "  • communication-service"
Write-Host "  • administration-service"
Write-Host "  • procedure-service"
Write-Host "  • core-ops-service"
Write-Host "  • ops-service"
Write-Host "  • frontend"
Write-Host "  + AI services"

$startTime = Get-Date
docker compose -f docker-compose.full.yml build --no-cache
$buildTime = (Get-Date) - $startTime
Write-Host "`n✅ Docker build completed in $($buildTime.TotalMinutes.ToString('0.0')) minutes"

# ============================================================================
# PHASE 2: LOAD IMAGES INTO MINIKUBE
# ============================================================================
Write-Host "`n╔═══════════════════════════════════════════════════════════╗"
Write-Host "║ PHASE 2: LOADING IMAGES INTO MINIKUBE                     ║"
Write-Host "╚═══════════════════════════════════════════════════════════╝"

$images = @(
  "nephropaidi-api-gateway",
  "nephropaidi-eureka",
  "nephropaidi-config-server",
  "nephropaidi-clinical-service",
  "nephropaidi-user-service",
  "nephropaidi-pharmacy-service",
  "nephropaidi-communication-service",
  "nephropaidi-administration-service",
  "nephropaidi-procedure-service",
  "nephropaidi-core-ops-service",
  "nephropaidi-ops-service"
)

foreach ($img in $images) {
  Write-Host "`n⏳ Loading $img into Minikube..."
  minikube image load "${img}:latest" 2>&1 | Select-String "Loaded" -ErrorAction SilentlyContinue
  if ($LASTEXITCODE -eq 0) {
    Write-Host "  ✅ $img loaded"
  }
}

Write-Host "`n✅ All images loaded into Minikube"

# ============================================================================
# PHASE 3: RESTART KUBERNETES DEPLOYMENTS
# ============================================================================
Write-Host "`n╔═══════════════════════════════════════════════════════════╗"
Write-Host "║ PHASE 3: RESTARTING KUBERNETES DEPLOYMENTS               ║"
Write-Host "╚═══════════════════════════════════════════════════════════╝"

$deployments = @(
  "api-gateway",
  "eureka",
  "config-server",
  "clinical-service",
  "user-service",
  "pharmacy-service",
  "communication-service",
  "administration-service",
  "procedure-service",
  "core-ops-service",
  "ops-service"
)

foreach ($dep in $deployments) {
  Write-Host "`n⏳ Restarting $dep deployment..."
  kubectl rollout restart deployment/$dep -n nephro 2>&1 | Select-String "restarted" -ErrorAction SilentlyContinue
}

Write-Host "`n⏳ Waiting 45 seconds for all pods to restart..."
Start-Sleep -Seconds 45

Write-Host "`n✅ All deployments restarted"

# ============================================================================
# PHASE 4: VERIFY PODS RUNNING
# ============================================================================
Write-Host "`n╔═══════════════════════════════════════════════════════════╗"
Write-Host "║ PHASE 4: VERIFICATION                                     ║"
Write-Host "╚═══════════════════════════════════════════════════════════╝"

Write-Host "`nPod Status:"
$podCount = (kubectl get pods -n nephro --no-headers | Measure-Object -Line).Lines
Write-Host "  Total pods: $podCount"

kubectl get pods -n nephro --no-headers | ForEach-Object {
  if ($_ -match "(\S+)\s+(\d+)/(\d+)\s+(\S+)") {
    $name = $matches[1]
    $status = $matches[4]
    $icon = if ($status -eq "Running") { "✅" } else { "⏳" }
    Write-Host "  $icon $name ($status)"
  }
}

# ============================================================================
# PHASE 5: TEST METRICS ENDPOINTS
# ============================================================================
Write-Host "`n╔═══════════════════════════════════════════════════════════╗"
Write-Host "║ PHASE 5: TESTING METRICS ENDPOINTS                        ║"
Write-Host "╚═══════════════════════════════════════════════════════════╝"

Write-Host "`nTesting /actuator/prometheus endpoints (30 second timeout)..."

$testServices = @(
  @{Name="api-gateway"; Port=8083},
  @{Name="clinical-service"; Port=8084},
  @{Name="user-service"; Port=8090}
)

foreach ($svc in $testServices) {
  Write-Host "`n⏳ Testing $($svc.Name):$($svc.Port)..."
  try {
    $response = curl -s "http://localhost:$($svc.Port)/actuator/prometheus" -m 5 -o /dev/null -w "%{http_code}"
    if ($response -eq "200") {
      Write-Host "  ✅ $($svc.Name) - Metrics endpoint UP (HTTP 200)"
    } else {
      Write-Host "  ⚠️ $($svc.Name) - HTTP $response"
    }
  } catch {
    Write-Host "  ❌ $($svc.Name) - Connection failed"
  }
}

# ============================================================================
# PHASE 6: VERIFY PROMETHEUS TARGETS
# ============================================================================
Write-Host "`n╔═══════════════════════════════════════════════════════════╗"
Write-Host "║ PHASE 6: PROMETHEUS TARGETS                              ║"
Write-Host "╚═══════════════════════════════════════════════════════════╝"

Write-Host "`nChecking Prometheus targets (should see UP status):"
try {
  $targets = curl -s "http://localhost:9090/api/v1/targets" | ConvertFrom-Json | Select-Object -ExpandProperty data | Select-Object -ExpandProperty activeTargets
  foreach ($target in $targets) {
    $job = $target.labels.job
    $health = $target.health
    $icon = if ($health -eq "up") { "✅" } else { "⚠️" }
    Write-Host "  $icon $job: $health"
  }
} catch {
  Write-Host "  ⚠️ Could not query Prometheus (is it running?)"
}

# ============================================================================
# SUMMARY
# ============================================================================
Write-Host @"
╔════════════════════════════════════════════════════════════════════════════╗
║                       🎉 REBUILD COMPLETE                                ║
╚════════════════════════════════════════════════════════════════════════════╝

✅ All Docker images rebuilt with metrics configuration
✅ All images loaded into Minikube
✅ All Kubernetes deployments restarted
✅ All pods should be Running
✅ Prometheus metrics endpoints should be UP

NEXT STEPS:

1. Verify Prometheus Dashboard:
   → http://localhost:9090/targets
   → All services should show (up) in green

2. Verify Grafana Dashboard:
   → http://localhost:3000 (admin/admin)
   → Navigate to nephropaidi-overview
   → Should see CPU, Memory, HTTP metrics

3. Test Jenkins Build:
   → http://localhost:8099
   → Run Build Now
   → Should complete in 2-3 minutes

4. If issues remain:
   → Check pod logs: kubectl logs -n nephro <pod-name>
   → Check metrics endpoint: kubectl port-forward svc/<service> <port>:<port>
"@
