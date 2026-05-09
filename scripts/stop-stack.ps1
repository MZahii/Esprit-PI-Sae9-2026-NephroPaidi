param(
  [string]$ComposeFile = "docker-compose.full.yml",
  [string]$ProjectName = "nephropaidi"
)

$ErrorActionPreference = "Stop"
docker compose -p $ProjectName -f $ComposeFile down --remove-orphans
Write-Host "Stack stopped."
