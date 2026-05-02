# NephroPaidi Login Fix Guide

## Problem Summary
After database changes on Neon, the frontend login is returning **HTTP 404 Not Found** when trying to reach `/api/auth/login`. This is because the User Service cannot connect to its database.

## Root Cause
The docker-compose configuration was using the **direct Neon host** instead of the **pooler host** for database connections. The pooler host provides better connection management and is required for proper operation.

### What was changed:
- **Old host**: `ep-cool-breeze-agqhhjlc.c-2.eu-central-1.aws.neon.tech`
- **New host**: `ep-cool-breeze-agqhhjlc-pooler.c-2.eu-central-1.aws.neon.tech` (note: `-pooler` suffix)

## Changes Made

### 1. User Service (Port 8090)
Updated `docker-compose.full.yml` user-service datasource:
- Fixed database host to use pooler
- Added environment variable support (`${SPRING_DATASOURCE_URL}`)
- Added proper JPA/Hibernate configuration
- Added health check for container monitoring
- Added proper logging configuration

**Key setting:**
```yaml
SPRING_DATASOURCE_URL: ${SPRING_DATASOURCE_URL:-jdbc:postgresql://ep-cool-breeze-agqhhjlc-pooler.c-2.eu-central-1.aws.neon.tech:5432/nephros_user_service_db?sslmode=require&channel_binding=require}
```

### 2. Core OPS Service (Port 8086)
Updated the Python service database connection to also use the pooler host.

## How to Apply These Changes

### Option A: Use the Updated docker-compose.full.yml
The changes have been made to your `docker-compose.full.yml`. To apply them:

```bash
# 1. Stop all running containers
docker compose -f docker-compose.full.yml down

# 2. Rebuild and start services
docker compose -f docker-compose.full.yml up -d --build user-service

# Wait for the service to start (should see health checks passing)
sleep 15

# 3. Test the login endpoint
curl http://localhost:8083/api/auth/login

# 4. Check service logs to ensure it's connecting properly
docker logs nephro-user-service
```

### Option B: Verify Your Environment
If you have a `.env` file, ensure it contains:
```env
# Database credentials (already configured in docker-compose)
SPRING_DATASOURCE_URL=jdbc:postgresql://ep-cool-breeze-agqhhjlc-pooler.c-2.eu-central-1.aws.neon.tech:5432/nephros_user_service_db?sslmode=require&channel_binding=require
SPRING_DATASOURCE_USERNAME=neondb_owner
SPRING_DATASOURCE_PASSWORD=npg_Zb1Kda5LPlin
```

## Verification Checklist

After applying the changes, verify:

1. **User Service is running:**
   ```bash
   curl http://localhost:8090/actuator/health
   # Should return: {"status":"UP"}
   ```

2. **API Gateway is routing correctly:**
   ```bash
   curl -X POST http://localhost:8083/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"identifier":"test","password":"test"}'
   # Should return a 401 error (invalid credentials) not 404
   ```

3. **Frontend can reach the backend:**
   - Open http://localhost:4200/login
   - Try logging in with valid credentials
   - Should see proper authentication error or successful login, not 404

4. **Check service logs:**
   ```bash
   docker logs nephro-user-service | tail -50
   docker logs nephro-api-gateway | tail -50
   ```

## If Issues Persist

### Issue: "Connection refused" to database
- Verify your Neon credentials are correct
- Check if the database `nephros_user_service_db` exists on Neon
- Verify network connectivity: `ping ep-cool-breeze-agqhhjlc-pooler.c-2.eu-central-1.aws.neon.tech`

### Issue: "404 Not Found" still appearing
- Ensure User Service container is running: `docker ps | grep user-service`
- Check if API Gateway is routing to user-service: `docker logs nephro-api-gateway`
- Verify Eureka discovery: `curl http://localhost:8761/eureka/apps`

### Issue: Migration errors
- The service has `ddl-auto: validate` set, so it won't auto-create tables
- Ensure your database schema is properly initialized
- Check Flyway migrations in user-service

## Database Requirements

The user-service expects a PostgreSQL database named `nephros_user_service_db` with proper schema. The database should contain:
- user table
- role table
- authentication-related tables (set up via Flyway migrations)

## Important Notes

1. **No Breaking Changes**: These changes only fix the database connection host
2. **Environment Variable Support**: You can override any of these via `.env` or environment variables
3. **Health Checks Added**: User Service now has proper health checks for better monitoring
4. **Connection Pooling**: The pooler host handles connection management more efficiently

## Next Steps

1. Apply the changes with `docker compose up -d --build user-service`
2. Wait 30-40 seconds for the service to fully start
3. Run the verification checklist above
4. Test login on the frontend
5. If issues occur, check the logs and database connectivity

For questions or issues, check the service logs with: `docker logs [container-name]`
