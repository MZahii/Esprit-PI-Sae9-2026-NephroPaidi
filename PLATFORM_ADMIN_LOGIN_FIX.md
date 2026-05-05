# NephroPaidi platform-admin Login Issue - Diagnostic Report & Fix

## Problem Identified ✓

**Status**: Account exists in Keycloak but NOT in Neon database

### Root Cause
The `platform-admin` account is defined in the Keycloak realm configuration file (`nephrospaidi-realm.json`) and loaded into Keycloak when it starts. However, **this user does not exist in the Neon database** (`nephros_user_service_db`).

### Why It Fails
1. Frontend sends login request: `platform-admin / Admin123!`
2. API Gateway routes to User Service
3. User Service AuthService calls: `userRepository.findByIdentifier("platform-admin")`
4. Database query finds **NO** matching user
5. `orElseThrow()` exception is raised
6. Server returns 500 error (Internal Server Error)

### Why Other Accounts Work
The `doc1` account exists in **BOTH** places:
- ✅ In Keycloak (realm configuration or manually created)
- ✅ In Neon database `nephros_user_service_db`

## The Solution

You need to **create the platform-admin user in the Neon database**. There are two approaches:

### Approach 1: SQL Script (Direct Database)

Run this SQL directly in your Neon console:

```sql
-- First, find the keycloak_id for platform-admin
-- You can get this from Keycloak Admin Console at:
-- http://localhost:8080/admin/master/console/
-- Then go to: nephrospaidi realm -> Users -> platform-admin -> copy the ID

-- INSERT the user into the database
-- Replace KEYCLOAK_ID_HERE with the actual ID from Keycloak
INSERT INTO users (
    keycloak_id,
    username,
    email,
    first_name,
    last_name,
    cin,
    role,
    account_status,
    enabled,
    preferred_language,
    notifications_enabled,
    theme,
    must_change_password,
    is_deleted,
    created_at,
    updated_at
) VALUES (
    'KEYCLOAK_ID_HERE',  -- Replace with actual keycloak_id from Keycloak console
    'platform-admin',
    'platform-admin@nephrospaidi.local',
    'Platform',
    'Admin',
    'ADMIN-001',  -- or any unique CIN value
    'ADMIN',
    'ACTIVE',  -- Not PENDING_CONTRACT so login works immediately
    true,
    'en',
    true,
    'light',
    false,
    false,
    NOW(),
    NOW()
)
ON CONFLICT (username) DO NOTHING;  -- Skip if already exists
```

### Approach 2: Get Keycloak ID (Required First Step)

1. **Open Keycloak Admin Console**: `http://localhost:8080/admin/master/console/`
2. **Login**: Use `admin / admin123`
3. **Navigate**:
   - Click on the realm dropdown → Select **nephrospaidi**
   - Left menu → Click **Users**
   - Find **platform-admin**
   - Copy the **User ID** (UUID format)
4. **Use this ID** in the SQL script above, replacing `KEYCLOAK_ID_HERE`

### Approach 3: Using Database Admin Tools (Neon Console)

1. Go to your Neon console: https://console.neon.tech
2. Select your project
3. Open **SQL Editor**
4. Use the SQL script from Approach 1 (with correct keycloak_id)

## Step-by-Step Fix Process

### Step 1: Get the platform-admin Keycloak ID
```
1. Open http://localhost:8080/admin/master/console/
2. Login with: admin / admin123
3. Select realm: nephrospaidi
4. Go to Users → platform-admin
5. Copy the ID (looks like: 550e8400-e29b-41d4-a716-446655440000)
```

### Step 2: Insert into Database
Use one of the approaches above with the keycloak_id you collected

### Step 3: Test Login
```
1. Open http://localhost:4200/login
2. Enter: platform-admin / Admin123!
3. Should now work!
```

## Verification Checklist

After applying the fix, verify that platform-admin can:

✅ Login successfully
✅ See the message "Email is not verified" or get redirected (check email verification status in Keycloak)
✅ Appear in the database:
```sql
SELECT id, username, email, role, account_status, enabled
FROM users
WHERE username = 'platform-admin';
```

## Important Keycloak Note

The Keycloak realm JSON file loads the user definition but **does NOT automatically sync to your application database**. You must:

1. **Keycloak**: Manages authentication (passwords, email verification, OAuth)
2. **Neon Database**: Manages user account metadata (role, account_status, preferences)

These are **two separate systems** that must be kept in sync manually or via an API call.

## Preventing This in Future

When you add a new user in Keycloak, you must also:
1. Create them in the Neon database with matching `keycloak_id`
2. Set proper `role`, `account_status`, and `enabled` fields
3. Ensure both usernames/emails match exactly

## SQL to Check Current State

Run these queries in Neon console:

```sql
-- Check if platform-admin exists in database
SELECT * FROM users WHERE username = 'platform-admin';

-- Count how many users are in the database
SELECT COUNT(*) as total_users FROM users;

-- Check doc1 for comparison
SELECT id, username, email, keycloak_id, role, account_status, enabled
FROM users
WHERE username = 'doc1';
```

## Questions?

If the fix doesn't work:
1. Verify keycloak_id is correct (check Keycloak console)
2. Check for duplicate usernames in database
3. Verify account_status is 'ACTIVE' not 'PENDING_CONTRACT'
4. Check enabled column is 'true'
5. Check Docker logs: `docker logs nephro-user-service`
