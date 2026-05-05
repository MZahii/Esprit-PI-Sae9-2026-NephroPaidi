# System Architecture: Keycloak ↔ Neon Database Synchronization

## Architecture Overview

Your NephroPaidi system has a **dual authentication system**:

```
┌──────────────────────────────────────────────────────────────┐
│                     Frontend (Angular)                        │
│                   http://localhost:4200                       │
└───────────────────────┬──────────────────────────────────────┘
                        │ Login Request
                        ↓
┌──────────────────────────────────────────────────────────────┐
│           API Gateway (Spring Cloud Gateway)                  │
│                  http://localhost:8083                        │
│                Routes /api/auth/** to User Service            │
└───────────────────────┬──────────────────────────────────────┘
                        │
                        ↓
┌──────────────────────────────────────────────────────────────┐
│  User Service (Spring Boot) - AuthService.login()            │
│         http://localhost:8090                                │
│                                                              │
│  Step 1: userRepository.findByIdentifier("platform-admin")   │
│          ↓ Queries Neon Database for user record             │
│          ↓ IF NOT FOUND → Returns empty → Throws exception  │
│                                                              │
│  Step 2: enforceEmailVerification(user)                      │
│          ↓ Checks Keycloak if email is verified             │
│                                                              │
│  Step 3: requestTokenFromKeycloak(username, password)        │
│          ↓ Validates credentials against Keycloak           │
│          ↓ Gets JWT tokens from Keycloak                    │
│                                                              │
│  Step 4: Build and return LoginResponse                      │
└─────────────┬──────────────────────────────────────┬────────┘
              │                                      │
              ↓                                      ↓
        ┌──────────────────┐              ┌─────────────────┐
        │ NEON DATABASE    │              │  KEYCLOAK       │
        │ nephros_user_*db │              │ (Port 8080)     │
        │                  │              │                 │
        │ users table:     │              │ realms:         │
        │ - id             │              │ - nephrospaidi  │
        │ - username  ◄──────────────────► - users          │
        │ - email     ◄──────────────────► - roles          │
        │ - keycloak_id    │              │ - sessions      │
        │ - role           │              │                 │
        │ - account_status │              │ Authentication: │
        │ - enabled        │              │ - Passwords     │
        │ - created_at     │              │ - Email verify  │
        │ - updated_at     │              │ - MFA           │
        │ - ...            │              │ - JWT tokens    │
        └──────────────────┘              └─────────────────┘
```

## The Mismatch Problem

**PROBLEM**: platform-admin exists in ONE system but not the OTHER

```
Keycloak (✓ HAS platform-admin)    Neon Database (✗ NO platform-admin)
├── realm: nephrospaidi             ├── users table
│   ├── users                       │   ├── doc1 ✓
│   │   ├── platform-admin ✓         │   └── [other users]
│   │   ├── doc1 ✓                   └── [platform-admin missing!]
│   │   └── [others]
│   └── roles
└── ...
```

**SOLUTION**: Create the missing user in Neon Database with the correct keycloak_id

## Key Differences Between Accounts

### platform-admin (BROKEN)
```
Keycloak:               Neon Database:
├─ Username: platform-admin    ├─ NOT FOUND ✗
├─ Email: platform-admin@...   
├─ First Name: Platform        
├─ Last Name: Admin            
├─ Realm Role: ADMIN           
├─ Email Verified: true        
└─ User ID: [UUID] ← NEED THIS
```

### doc1 (WORKING)
```
Keycloak:               Neon Database:
├─ Username: doc1       ├─ Username: doc1 ✓
├─ Email: doc1@...      ├─ Email: doc1@... ✓
├─ First Name: Doctor   ├─ First Name: Doctor ✓
├─ Last Name: One       ├─ Last Name: One ✓
├─ Realm Role: DOCTOR   ├─ Role: DOCTOR ✓
├─ Email Verified: true ├─ Account Status: ACTIVE ✓
└─ User ID: [UUID]      └─ Keycloak ID: [UUID] ✓
```

## Login Flow - What Goes Wrong

```
When user tries: platform-admin / Admin123!

1. Frontend → POST /api/auth/login
   {"identifier": "platform-admin", "password": "Admin123!"}

2. User Service receives request

3. ❌ FAILS HERE:
   userRepository.findByIdentifier("platform-admin")
   → Query: SELECT * FROM users WHERE username='platform-admin' OR email='platform-admin@...'
   → Result: EMPTY (no record found)
   → orElseThrow() throws exception
   → Returns 500 error to frontend

4. ✅ WOULD CONTINUE IF FOUND:
   If user existed:
   ├─ Check if enabled: true ✓
   ├─ enforceEmailVerification(user)
   │  └─ Get state from Keycloak: emailVerified=true ✓
   ├─ requestTokenFromKeycloak("platform-admin", "Admin123!")
   │  └─ Keycloak validates password ✓
   │  └─ Returns JWT tokens ✓
   └─ Return LoginResponse with tokens
   
5. Frontend receives tokens → Redirects to dashboard ✓
```

## How to Verify the Fix

### Before Fix
```sql
SELECT COUNT(*) FROM users WHERE username = 'platform-admin';
-- Returns: 0 ❌
```

### After Fix
```sql
SELECT id, username, email, keycloak_id, role, account_status, enabled 
FROM users 
WHERE username = 'platform-admin';
-- Returns: (some_id, platform-admin, platform-admin@nephrospaidi.local, uuid, ADMIN, ACTIVE, true) ✓
```

## Required Information to Complete the Fix

You need to gather:

```
1. From Keycloak Admin Console:
   ✓ platform-admin's User ID (UUID)
   ✓ Confirm email: platform-admin@nephrospaidi.local
   ✓ Confirm roles: ADMIN

2. From Current System:
   ✓ Check what doc1's keycloak_id looks like (format reference)
   ✓ Verify account_status values (PENDING_CONTRACT vs ACTIVE)
```

## Next Steps

1. **Get Keycloak ID**: Open Keycloak admin console and copy platform-admin's user ID
2. **Run SQL**: Execute the INSERT script from PLATFORM_ADMIN_LOGIN_FIX.md
3. **Test**: Try logging in with platform-admin / Admin123!
4. **Verify**: Check database record was inserted correctly

## Prevention: Dual-System Awareness

✅ **Always remember**: 
- Keycloak = Authentication (passwords, email verification)
- Neon Database = User Metadata (roles, status, preferences)

When adding a new user:
```
Step 1: Create in Keycloak
   └─ Get the generated User ID

Step 2: Create in Neon Database
   ├─ Use the Keycloak User ID as keycloak_id
   ├─ Copy username and email from Keycloak
   ├─ Set role (must match Keycloak realm role)
   ├─ Set account_status = 'ACTIVE'
   └─ Set enabled = true

Step 3: Verify both systems have matching data
```
