# Frontend Test Guide

This frontend is an Angular 21 app. Keep this file focused on how to run and validate it before the jury.

## Prerequisites

- Node.js 20+ and npm
- Backend gateway running at `http://localhost:8083`
- Frontend dependencies installed with `npm install`

## Start the app

From this folder:

```powershell
npm install
npm start
```

Open `http://localhost:4200`.

## Run all frontend tests

```powershell
npm run test -- --watch=false
```

This runs the Angular spec files in `src/app/**/*.spec.ts`.

## Run one frontend test file

Use this when you want to check one feature fast:

```powershell
npm run test -- --watch=false --include src/app/core/services/appointments-api.service.spec.ts
```

Replace the spec path with the file you want to verify.

## Build check

```powershell
npm run build
```

Run the build after tests to catch template and TypeScript issues before the demo.

## What to test first for the jury

- Auth/session flows
- API service specs for the most used screens
- Route guards and interceptor behavior
- Dashboard and booking/appointment screens
- Any service that talks to the gateway

## Useful pattern for new specs

- Use `HttpClientTestingModule` for service tests.
- Mock the backend with `HttpTestingController`.
- Keep tests focused on one behavior per spec.
- Prefer a small number of meaningful tests over many trivial ones.
