param()
Write-Output "Starting infra and services for local dev..."
Push-Location BackEnd
docker compose -f docker-compose.infra.yml up -d
Pop-Location
Write-Output "Infrastructure started. Start services from IntelliJ or run mvnw in microservice folders as needed."
