# DevOps Phase 1 - Jenkins CI + SonarQube

This phase gives you:
- CI pipeline for backend + frontend
- Optional frontend tests
- SonarQube scan with quality gate blocking

## 1. What was added in code

- `Jenkinsfile` at repo root
- `sonar-project.properties` at repo root

## 2. One-time Jenkins setup (UI)

Install these Jenkins plugins:
- Pipeline
- Git
- Credentials Binding
- SonarQube Scanner
- Quality Gates
- NodeJS

Then configure:

1. `Manage Jenkins` -> `Global Tool Configuration`
- Add JDK named `jdk17`
- Add NodeJS named `node20` (Node 20.x)

2. `Manage Jenkins` -> `System` -> `SonarQube servers`
- Name: `sonarqube`
- Server URL: `http://<your-sonarqube-host>:9000`
- Token: add Jenkins secret text credential with id `sonarqube-token`

3. Create pipeline job
- Type: `Pipeline`
- Definition: `Pipeline script from SCM`
- SCM: `Git`
- Script Path: `Jenkinsfile`

## 3. One-time SonarQube setup (UI)

1. Create project in SonarQube:
- Project key: `nephropaidi`
- Name: `NephroPaidi`

2. Create token:
- `My Account` -> `Security` -> Generate token
- Copy token value and store in Jenkins secret text credential id `sonarqube-token`

3. Set quality gate:
- Use default `Sonar way`, or create custom gate and assign it to `nephropaidi`

## 4. Required agent/runtime tools

Jenkins build node must have:
- Git
- Docker (optional for later phases)
- JDK 17
- Maven wrapper support (`./mvnw` uses local `.mvn`)
- Node.js 20 + npm
- `sonar-scanner` CLI available in PATH

## 5. How to run

Run Jenkins job with:
- `RUN_FRONTEND_TESTS=false` for faster first runs
- `RUN_SONAR=true` to enforce quality gate

## 6. Notes

- Frontend tests in headless mode may need Chrome/Chromium on the Jenkins node.
- If your Jenkins agent is Windows-only, we can add a Windows-compatible Jenkinsfile in Phase 1.1.
