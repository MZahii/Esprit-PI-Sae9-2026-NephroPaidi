# PiDev26Nephro

Team setup guide for running the project locally (backend + frontend).

## 1. Prerequisites

- Docker Desktop (running)
- Java 17
- Maven (or IntelliJ with Maven support)
- Node.js 20+ and npm
- Angular CLI (`npm i -g @angular/cli`) or use `npx ng`
- IntelliJ IDEA

## 2. Project Architecture

### Backend (`BackEnd/`)

- `eureka/`  
  Service discovery (port `8761`).
- `api-gateway/`  
  Entry point for frontend and API routing (port `8083`).
- `keycloak/`  
  Realm import files for authentication.
- `microservices/`  
  Domain services:
  - `user-service` (port `8090`) - users, auth-related APIs
  - `ops-service` (port `8082`)
  - `clinical-service` (port `8084`)
  - `communication-service` (port `8085`)
  - `core-ops-service` (port `8086`)
  - `patient-service` (port `8087`)
  - `pharmacy-service` (port `8088`)
  - `procedure-service` (port `8089`)
- `docker-compose.infra.yml`  
  Infra containers (Keycloak DB, Keycloak, Eureka, API Gateway).

### Frontend (`FrontEnd/duralux-admin/`)

Angular application (standalone components, SCSS, route guards).

- `src/app/core/`  
  Shared logic: auth services, Keycloak integration, guards.
- `src/app/layouts/`  
  Layout shells:
  - `public-layout`
  - `backoffice-layout`
  - `frontoffice-layout`
- `src/app/pages/`  
  Feature pages grouped by area:
  - `public/` (home, about, services, login, etc.)
  - `backoffice/` (dashboard, create HR/staff, staff list)
  - `frontoffice/` (guardian area)
- `src/environments/environment.ts`  
  Frontend API base URL (`http://localhost:8083`).

## 3. How We Run the Project (Recommended Team Flow)

Use this exact order.

### Step 1: Start Docker Desktop

Open Docker Desktop and make sure engine status is **Running**.

### Step 2: Start backend infrastructure

From project root:

```powershell
cd BackEnd
# first time only: copy .env.example to .env and set shared Keycloak DB values
# cp .env.example .env
docker compose -f docker-compose.infra.yml up -d
```

This starts Keycloak, Eureka, and infra dependencies.

Important:
- Keycloak now reads DB credentials from `BackEnd/.env`.
- To share Keycloak users/realm data across team members, use the same shared Postgres/Neon values in `.env`.
- The API Gateway container is now behind the `container-gateway` profile, so `docker compose -f docker-compose.infra.yml up -d` will not reserve port `8083`.
- If you want local Keycloak DB only (not shared), run:

```powershell
docker compose --profile local-keycloak-db -f docker-compose.infra.yml up -d
```

### Step 3: Run backend apps from IntelliJ

Open the project in IntelliJ and run:

1. `ApiGatewayApplication`
2. `UserServiceApplication`

Important:
- The normal infra command no longer starts the gateway container, so `ApiGatewayApplication` can run on `8083` from IntelliJ without conflict.
- If you explicitly start the gateway container profile, stop it before running `ApiGatewayApplication` in IntelliJ:

```powershell
docker compose -f docker-compose.infra.yml --profile container-gateway down
```

### Step 4: Start frontend

From project root:

```powershell
cd FrontEnd/duralux-admin
npm install
ng serve
```

If Angular CLI is not global:

```powershell
npx ng serve
```

## 4. URLs to Open

- Frontend: `http://localhost:4200`
- API Gateway: `http://localhost:8083`
- User Service Swagger: `http://localhost:8090/swagger-ui/index.html`
- Eureka: `http://localhost:8761`
- Keycloak: `http://localhost:8080`

## 5. Quick Troubleshooting

- Port already in use (`8083`, `8090`, `4200`): stop the app/container using that port, then restart.
- Frontend cannot call backend: confirm `environment.ts` points to `http://localhost:8083`.
- Gateway routes not working: verify Eureka is up and services are registered.
- Auth issues: confirm Keycloak container is running and realm import succeeded.

## 6. Branded Keycloak Verification Emails

Keycloak email verification is now themed with a custom template:

- Theme path: `BackEnd/keycloak/themes/nephrospaidi/email`
- Realm setting: `emailTheme: "nephrospaidi"` in `BackEnd/keycloak/realm/nephrospaidi-realm.json`
- Docker mount: `./keycloak/themes:/opt/keycloak/themes` in infra compose files

How to apply changes:

```powershell
cd BackEnd
docker compose -f docker-compose.infra.yml up -d --force-recreate keycloak
```

How to customize branding:

1. Edit layout/style: `BackEnd/keycloak/themes/nephrospaidi/email/html/template.ftl`
2. Edit verification subject/body text: `BackEnd/keycloak/themes/nephrospaidi/email/messages/messages_en.properties`
3. Recreate Keycloak container with the command above.

How to test email rendering:

1. Open Keycloak Admin (`http://localhost:8080`) and go to realm `nephrospaidi`.
2. Ensure SMTP is configured in `Realm settings -> Email`.
3. Open a user and click `Send verify email`.
4. Check the received email (subject, branding, button, footer text).

## 7. Daily Start/Stop Commands

Start infra:

```powershell
cd BackEnd
docker compose -f docker-compose.infra.yml up -d
```

Stop infra:

```powershell
cd BackEnd
docker compose -f docker-compose.infra.yml down
```
