param(
  [string]$ComposeFile = "docker-compose.full.yml",
  [string]$ProjectName = "nephropaidi"
)

$ErrorActionPreference = "Stop"

function Wait-Url {
  param(
    [Parameter(Mandatory = $true)][string]$Url,
    [int]$TimeoutSec = 180,
    [int]$RetrySec = 5
  )

  $end = (Get-Date).AddSeconds($TimeoutSec)
  while ((Get-Date) -lt $end) {
    try {
      Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5 | Out-Null
      Write-Host "Ready: $Url"
      return
    } catch {
      Start-Sleep -Seconds $RetrySec
    }
  }

  throw "Timeout while waiting for $Url"
}

$env:COMPOSE_PARALLEL_LIMIT = "2"
Write-Host "COMPOSE_PARALLEL_LIMIT=$($env:COMPOSE_PARALLEL_LIMIT) (CPU guard)"

Write-Host "Step 1/4: starting core infra (eureka, keycloak, rabbitmq, config-server)..."
docker compose -p $ProjectName -f $ComposeFile up -d --build eureka keycloak rabbitmq config-server
Wait-Url -Url "http://localhost:8761/actuator/health" -TimeoutSec 300
Wait-Url -Url "http://localhost:8888/actuator/health" -TimeoutSec 300
Wait-Url -Url "http://localhost:8080/realms/master/.well-known/openid-configuration" -TimeoutSec 300

Write-Host "Step 2/4: starting backend microservices (without gateway/frontend)..."
docker compose -p $ProjectName -f $ComposeFile up -d --build ai-triage-service user-service administration-service communication-service clinical-service ops-service core-ops-service pharmacy-service procedure-service
Wait-Url -Url "http://localhost:8000/health" -TimeoutSec 300
Wait-Url -Url "http://localhost:8090/actuator/health" -TimeoutSec 300
Wait-Url -Url "http://localhost:8087/actuator/health" -TimeoutSec 300

Write-Host "Step 3/4: starting api-gateway after services are discoverable..."
docker compose -p $ProjectName -f $ComposeFile up -d --build api-gateway
Wait-Url -Url "http://localhost:8083/actuator/health" -TimeoutSec 240

Write-Host "Step 4/4: starting frontend..."
docker compose -p $ProjectName -f $ComposeFile up -d --build frontend

Write-Host "Done. Stack is starting in priority order."
docker compose -p $ProjectName -f $ComposeFile ps
