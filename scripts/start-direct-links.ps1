$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$logDir = Join-Path $root "runtime\port-forwards"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

$forwards = @(
  @{ Name = "grafana";    Namespace = "nephro"; Service = "grafana";    LocalPort = 3000; RemotePort = 3000 },
  @{ Name = "prometheus"; Namespace = "nephro"; Service = "prometheus"; LocalPort = 9090; RemotePort = 9090 },
  @{ Name = "keycloak";   Namespace = "nephro"; Service = "keycloak";   LocalPort = 8080; RemotePort = 8080 },
  @{ Name = "frontend";   Namespace = "nephro"; Service = "frontend";   LocalPort = 8081; RemotePort = 80   },
  @{ Name = "gateway";    Namespace = "nephro"; Service = "api-gateway";LocalPort = 8083; RemotePort = 8083 }
)

foreach ($forward in $forwards) {
  $alreadyListening = Get-NetTCPConnection -State Listen -LocalPort $forward.LocalPort -ErrorAction SilentlyContinue
  if ($alreadyListening) {
    Write-Host "Skipping $($forward.Name): localhost:$($forward.LocalPort) is already in use."
    continue
  }

  $logFile = Join-Path $logDir "$($forward.Name).log"
  $cmd = "kubectl port-forward -n $($forward.Namespace) svc/$($forward.Service) $($forward.LocalPort):$($forward.RemotePort)"

  Start-Process powershell `
    -ArgumentList @(
      "-NoProfile",
      "-WindowStyle", "Hidden",
      "-Command", "$cmd *> '$logFile'"
    ) `
    -WorkingDirectory $root `
    -WindowStyle Hidden | Out-Null

  Write-Host "Started $($forward.Name) on http://localhost:$($forward.LocalPort)"
}

Write-Host ""
Write-Host "Direct local links:"
Write-Host "  Jenkins    -> http://localhost:8099"
Write-Host "  SonarQube  -> http://localhost:9000"
Write-Host "  Grafana    -> http://localhost:3000"
Write-Host "  Prometheus -> http://localhost:9090"
Write-Host "  Keycloak   -> http://localhost:8080"
Write-Host "  Frontend   -> http://localhost:8081"
Write-Host "  Gateway    -> http://localhost:8083/actuator/health"
