# Local Docker Rebuild and Kubernetes Rollout Helper
# Purpose: rebuild local images, optionally load them into Minikube, restart workloads,
# and run a lightweight verification pass.
# Note: this is a local helper only. The Sprint 3 jury path must target a kubeadm cluster.

$banner = @"
╔════════════════════════════════════════════════════════════════════════════╗
║                 LOCAL REBUILD + KUBERNETES ROLLOUT HELPER                ║
║         Use for local validation only, not as the final kubeadm flow     ║
╚════════════════════════════════════════════════════════════════════════════╝
"@
Write-Host $banner

Set-Location (Split-Path -Parent $PSScriptRoot)
Write-Host "Working directory: $(Get-Location)"

function Get-JsonStatusCode {
  param(
    [Parameter(Mandatory = $true)][string]$Url,
    [int]$TimeoutSec = 5
  )

  try {
    $response = Invoke-WebRequest -UseBasicParsing -TimeoutSec $TimeoutSec $Url
    return $response.StatusCode
  } catch {
    return $null
  }
}

Write-Host "`n[1/5] Building Docker images from docker-compose.full.yml"
$buildStart = Get-Date
docker compose -f docker-compose.full.yml build --no-cache
if ($LASTEXITCODE -ne 0) {
  Write-Host "❌ Docker build failed."
  exit 1
}
$buildTime = (Get-Date) - $buildStart
Write-Host "✅ Docker build completed in $($buildTime.TotalMinutes.ToString('0.0')) minutes"

Write-Host "`n[2/5] Checking Kubernetes context"
try {
  $currentContext = (kubectl config current-context).Trim()
  Write-Host "Current context: $currentContext"
} catch {
  Write-Host "❌ Unable to read kubectl context."
  exit 1
}

try {
  kubectl cluster-info --request-timeout=10s | Out-Null
  Write-Host "✅ Kubernetes API reachable"
} catch {
  Write-Host "❌ Kubernetes API is not reachable."
  Write-Host "   Start your cluster first. For jury delivery, this should be kubeadm."
  exit 1
}

$useMinikube = $false
if ($currentContext -like "minikube*") {
  try {
    minikube profile list | Out-Null
    $useMinikube = $true
  } catch {
    $useMinikube = $false
  }
}

if ($useMinikube) {
  Write-Host "`n[3/5] Minikube detected, loading local images"
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
    "nephropaidi-ops-service",
    "nephropaidi-frontend"
  )

  foreach ($img in $images) {
    Write-Host "⏳ Loading $($img):latest"
    minikube image load "${img}:latest" | Out-Null
    if ($LASTEXITCODE -eq 0) {
      Write-Host "  ✅ $img loaded"
    } else {
      Write-Host "  ⚠️ Failed to load $img"
    }
  }
} else {
  Write-Host "`n[3/5] Non-Minikube context detected"
  Write-Host "ℹ️ Skipping local image load. For kubeadm, push images to a registry visible from the nodes."
}

Write-Host "`n[4/5] Restarting Kubernetes deployments"
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
  "ops-service",
  "frontend"
)

foreach ($dep in $deployments) {
  Write-Host "⏳ Restarting deployment/$dep"
  kubectl rollout restart deployment/$dep -n nephro | Out-Null
}

Write-Host "⏳ Waiting 30 seconds for rollout start"
Start-Sleep -Seconds 30

Write-Host "`n[5/5] Verification"
try {
  kubectl get pods -n nephro
} catch {
  Write-Host "⚠️ Could not list pods."
}

Write-Host "`nLocal metrics spot-checks"
$testServices = @(
  @{ Name = "api-gateway"; Url = "http://localhost:8083/actuator/prometheus" },
  @{ Name = "clinical-service"; Url = "http://localhost:8084/actuator/prometheus" },
  @{ Name = "user-service"; Url = "http://localhost:8090/actuator/prometheus" }
)

foreach ($svc in $testServices) {
  $statusCode = Get-JsonStatusCode -Url $svc.Url
  if ($statusCode -eq 200) {
    Write-Host "✅ $($svc.Name): metrics endpoint reachable"
  } elseif ($statusCode) {
    Write-Host "⚠️ $($svc.Name): HTTP $statusCode"
  } else {
    Write-Host "⚠️ $($svc.Name): not reachable locally"
  }
}

Write-Host ""
Write-Host "Next steps:"
Write-Host "1. Run Jenkins with RUN_FRONTEND_TESTS=false unless the Vitest browser packages are installed."
Write-Host "2. For kubeadm, publish images to a registry and update manifests if nodes cannot see local images."
Write-Host "3. Verify ingress, Prometheus, and Grafana from the active cluster, not from Minikube-specific commands."
