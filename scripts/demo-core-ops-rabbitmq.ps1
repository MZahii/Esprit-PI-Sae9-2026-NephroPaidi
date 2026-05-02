param(
    [string]$GatewayBase = "http://localhost:8083",
    [string]$CoreOpsBase = "http://localhost:8086"
)

$ErrorActionPreference = "Stop"

function Show-Status {
    param(
        [string]$Name,
        [string]$Url
    )

    try {
        $resp = Invoke-WebRequest -Uri $Url -Method Get -TimeoutSec 10
        Write-Host ("{0}: {1}" -f $Name, [int]$resp.StatusCode)
    }
    catch {
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            Write-Host ("{0}: {1}" -f $Name, [int]$_.Exception.Response.StatusCode)
        }
        else {
            Write-Host ("{0}: ERROR" -f $Name)
        }
    }
}

Write-Host "=== Core-Ops Swagger/OpenAPI checks ==="
Show-Status -Name "coreops-direct-swagger" -Url "$CoreOpsBase/swagger-ui/index.html"
Show-Status -Name "coreops-direct-openapi" -Url "$CoreOpsBase/v3/api-docs"
Show-Status -Name "coreops-gateway-openapi" -Url "$GatewayBase/v3/api-docs/core-ops-service"
Show-Status -Name "coreops-gateway-swagger" -Url "$GatewayBase/swagger/core-ops-service/index.html"

Write-Host ""
Write-Host "=== RabbitMQ checks ==="
Show-Status -Name "rabbit-health-direct" -Url "$CoreOpsBase/api/core-ops/rabbitmq/health"
Show-Status -Name "rabbit-health-gateway" -Url "$GatewayBase/api/core-ops/rabbitmq/health"

$bindBody = @{ exchange = "core.ops.exchange"; queue = "core.ops.queue"; routing_key = "core.ops.#" } | ConvertTo-Json
$publishBody = @{
    exchange = "core.ops.exchange"
    routing_key = "core.ops.event"
    payload = @{ event = "jury-demo"; source = "core-ops-python"; at = (Get-Date).ToString("o") }
    persistent = $true
} | ConvertTo-Json -Depth 6
$consumeBody = @{ queue = "core.ops.queue"; auto_ack = $false } | ConvertTo-Json

$bind = Invoke-RestMethod -Method Post -Uri "$CoreOpsBase/api/core-ops/rabbitmq/bind-queue" -ContentType "application/json" -Body $bindBody
$publish = Invoke-RestMethod -Method Post -Uri "$CoreOpsBase/api/core-ops/rabbitmq/publish" -ContentType "application/json" -Body $publishBody
$consume = Invoke-RestMethod -Method Post -Uri "$CoreOpsBase/api/core-ops/rabbitmq/consume-once" -ContentType "application/json" -Body $consumeBody

Write-Host ""
Write-Host "=== RabbitMQ flow output ==="
Write-Host "bind:"
$bind | ConvertTo-Json -Depth 6
Write-Host "publish:"
$publish | ConvertTo-Json -Depth 6
Write-Host "consume:"
$consume | ConvertTo-Json -Depth 8

Write-Host ""
Write-Host "Demo completed."
