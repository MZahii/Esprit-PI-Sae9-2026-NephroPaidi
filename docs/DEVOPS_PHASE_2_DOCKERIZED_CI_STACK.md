# DevOps Phase 2 - Dockerized CI Stack

This phase provides a reproducible local DevOps stack:
- Jenkins
- SonarQube
- SonarQube PostgreSQL

## Files added/updated

- `docker-compose.devops.yml`
- `.env.example` (DevOps variables)

## 1) Start the DevOps stack

From repo root:

```powershell
docker compose -f docker-compose.devops.yml up -d
```

Check status:

```powershell
docker compose -f docker-compose.devops.yml ps
```

## 2) Access URLs

- Jenkins: `http://localhost:8099`
- SonarQube: `http://localhost:9000`

## 3) Stop the DevOps stack

```powershell
docker compose -f docker-compose.devops.yml down
```

If you want to delete persistent data too:

```powershell
docker compose -f docker-compose.devops.yml down -v
```

## 4) One-time Jenkins bootstrap

Get initial admin password:

```powershell
docker exec nephro-jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

Then in Jenkins install plugins:
- Pipeline
- Git
- Credentials Binding
- SonarQube Scanner for Jenkins
- NodeJS
- Pipeline: Stage View (optional)

## 5) One-time SonarQube bootstrap

- Login with default `admin/admin`
- Change password
- Create user token for Jenkins
- Configure the SonarQube Quality Gate webhook back to Jenkins:
  - URL: `http://jenkins:8080/sonarqube-webhook/`
  - Keep secret empty unless also configured in Jenkins

## 6) GitHub webhook note

The jury rubric also expects GitHub push automation. If Jenkins is only local, `localhost` is not enough for GitHub.

Use a public Jenkins URL or a temporary tunnel and configure:

- GitHub webhook URL: `https://<public-jenkins-url>/github-webhook/`
- Jenkins job trigger: `GitHub hook trigger for GITScm polling`

## 7) Port customization

Edit `.env` (copy from `.env.example`) to change:
- `JENKINS_HTTP_PORT`
- `SONARQUBE_PORT`
- `SONAR_DB_*`
