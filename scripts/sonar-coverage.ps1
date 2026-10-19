# Analyse SonarQube avec couverture réelle (JaCoCo + Vitest/LCOV)
# Prérequis : SonarQube démarré (docker compose -f docker-compose.devops.yml up -d sonarqube)
# Usage     : ./scripts/sonar-coverage.ps1 -Token <jeton SonarQube>
param(
  [Parameter(Mandatory = $true)][string]$Token,
  [string]$HostUrl = "http://localhost:9000",
  [string]$BackendKey = "nephropaidi-backend",
  [string]$FrontendKey = "nephropaidi-frontend"
)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$jacoco = "org.jacoco:jacoco-maven-plugin:0.8.12"

Write-Host "== Backend : tests + JaCoCo + analyse ==" -ForegroundColor Cyan
Push-Location (Join-Path $root "BackEnd")
mvn -B "$jacoco`:prepare-agent" verify "$jacoco`:report" `
  "-Dmaven.test.failure.ignore=true" `
  org.sonarsource.scanner.maven:sonar-maven-plugin:5.1.0.4751:sonar `
  "-Dsonar.host.url=$HostUrl" "-Dsonar.token=$Token" `
  "-Dsonar.projectKey=$BackendKey" "-Dsonar.projectName=NephroPaidi Backend" `
  "-Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml"
Pop-Location

Write-Host "== Frontend : tests Vitest + LCOV + analyse ==" -ForegroundColor Cyan
Push-Location (Join-Path $root "FrontEnd")
npm ci
npx ng test --watch=false --coverage
npx --yes sonarqube-scanner@4 "-Dsonar.host.url=$HostUrl" "-Dsonar.token=$Token" "-Dsonar.projectKey=$FrontendKey"
Pop-Location

Write-Host "Terminé : $HostUrl/dashboard?id=$BackendKey et $HostUrl/dashboard?id=$FrontendKey" -ForegroundColor Green
