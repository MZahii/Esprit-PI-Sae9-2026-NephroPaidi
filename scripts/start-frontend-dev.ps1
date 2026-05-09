param()

Write-Output "Starting frontend dev server with hot reload..."
Push-Location $PSScriptRoot/..
docker compose -p nephropaidi -f docker-compose.full.yml -f docker-compose.dev.yml up frontend
Pop-Location