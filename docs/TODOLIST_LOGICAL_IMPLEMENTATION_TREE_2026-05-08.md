# Todolist logique par dependances microservices

Date: 2026-05-08  
Objectif: organiser la todolist totale en blocs de travail coherents, selon les dependances metier et techniques entre microservices.

## 1. Principe du plan

Ce plan ne classe pas les taches par ordre de la todolist originale. Il les classe par dependances logiques.

Regle principale:

- si deux fonctionnalites partagent le meme patient, les memes statuts, les memes notifications, ou les memes ecrans metier, elles doivent etre implementees ensemble ou au minimum dans le meme sprint court.

Exemple:

- `procedure-service` ne doit pas etre developpe seul, car il depend fortement de `clinical-service`, `ops-service`, `communication-service`, `user-service` et du futur dossier medical commun.
- `pharmacy-service` doit etre lie a `ops-service` pour dialysis/hospitalization, a `clinical-service` pour prescriptions, et a `user-service` pour identifier nurse/pharmacist.

## 2. Vue globale en arbre

```text
NephroPaidi Implementation Plan
|
+-- 0. Socle transversal obligatoire
|   |
|   +-- Auth, roles, security
|   +-- User/staff identity
|   +-- Notifications
|   +-- Audit logs
|   +-- Shared enums/statuses
|   +-- Frontend shell/sidebar/settings
|
+-- 1. Patient clinical core
|   |
|   +-- Appointments
|   +-- Consultations
|   +-- Lab requests/results
|   +-- Prescriptions
|   +-- Medical dossier common timeline
|
+-- 2. Hospitalization and nurse operations
|   |
|   +-- Doctor hospitalization request
|   +-- Receptionist admission/room/bed
|   +-- Nurse todolist
|   +-- Nurse execution reports/signature
|   +-- Discharge document/follow-up
|
+-- 3. Pharmacy and stock workflows
|   |
|   +-- Medication prescription dispensing
|   +-- Equipment/dialysis/blood stock
|   +-- Nurse material pickup
|   +-- Pharmacist stock issue
|   +-- Supplier delivery
|   +-- Stock ledger and alerts
|
+-- 4. Procedure and surgery workflows
|   |
|   +-- Doctor surgery referral
|   +-- Surgeon offer/take FIFO
|   +-- Specific surgeon expiration
|   +-- Surgeon availability
|   +-- Pre-operation nurse/lab workflow
|   +-- Receptionist operation scheduling
|   +-- Operation checkin/checkout
|   +-- Post-operation report/follow-up
|
+-- 5. Communication layer
|   |
|   +-- Internal staff messaging
|   +-- Workflow notifications
|   +-- Role-based dashboards
|
+-- 6. Administration, HR, public content
|   |
|   +-- Editable public home page
|   +-- Staff contracts
|   +-- Resources 2D/3D
|   +-- Admin/HR statistics
|
+-- 7. Guardian/patient-facing features
    |
    +-- Guardian uploads
    +-- Guardian printing
    +-- Home supervision alerts
    +-- Discharge follow-up visibility
```

## 3. Step 0: socle transversal obligatoire

### Pourquoi commencer ici

Presque tous les workflows ont besoin des memes bases:

- savoir qui est connecte
- connaitre son role exact
- tracer qui a fait quoi
- envoyer des notifications
- afficher les bons menus
- proteger les routes frontend/backend

Si ce socle reste faible, les autres workflows vont etre developpes avec des champs texte libres, des roles mal controles, et des statuts differents selon microservice.

### Microservices concernes

```text
user-service
communication-service
administration-service
FrontEnd
Tous les autres microservices comme consommateurs
```

### Taches groupees

```text
0. Socle transversal
|
+-- 0.1 Authentification
|   |
|   +-- remember me
|   +-- forgot password
|   +-- 2FA
|   +-- session/token refresh
|
+-- 0.2 Roles et permissions
|   |
|   +-- ADMIN
|   +-- HR
|   +-- DOCTOR
|   +-- SURGEON
|   +-- NURSE
|   +-- PHARMACIST
|   +-- LAB_AGENT
|   +-- RECEPTIONIST
|   +-- GUARDIAN
|
+-- 0.3 Identite staff partagee
|   |
|   +-- staffId global
|   +-- displayName snapshot
|   +-- role snapshot
|   +-- service/department
|
+-- 0.4 Notifications role-based
|   |
|   +-- doctor notifications
|   +-- surgeon notifications
|   +-- nurse notifications
|   +-- pharmacist notifications
|   +-- receptionist notifications
|   +-- lab notifications
|
+-- 0.5 Audit logs
|   |
|   +-- actorId
|   +-- actorRole
|   +-- action
|   +-- entityType
|   +-- entityId
|   +-- before/after when needed
|
+-- 0.6 Frontend shell
    |
    +-- left sidebar by role
    +-- profile settings
    +-- security settings
    +-- language settings
```

### Dependances importantes

- La pharmacie a besoin de l'identite nurse/pharmacist.
- La procedure a besoin de l'identite doctor/surgeon/receptionist.
- Le dossier medical a besoin d'audit et de permissions.
- Les notifications doivent etre disponibles avant de finaliser procedure, lab, pharmacy et hospitalization.

### A faire avant les autres blocs

- Finaliser roles et guards frontend/backend.
- Avoir un modele fiable d'identite staff.
- Creer un service de notification utilisable par les autres microservices.
- Definir les enums partages ou au moins les aligner dans chaque microservice.

## 4. Step 1: patient clinical core

### Pourquoi ce bloc est central

Le `clinical-service` est le coeur logique du projet. Beaucoup de workflows commencent ou finissent par une decision clinique:

- rendez-vous
- consultation
- lab request
- prescription
- hospitalisation
- procedure/chirurgie
- dossier medical

### Microservices concernes

```text
clinical-service
ops-service
pharmacy-service
procedure-service
communication-service
FrontEnd
```

### Arbre logique

```text
1. Patient clinical core
|
+-- 1.1 Appointment workflow
|   |
|   +-- receptionist creates appointment
|   +-- doctor sees today's appointments
|   +-- doctor can start only today's appointment
|   +-- auto-cancel after 15 min
|   +-- reschedule with availability
|
+-- 1.2 Consultation workflow
|   |
|   +-- doctor opens consultation
|   +-- doctor records diagnosis/results
|   +-- consultation closes
|   +-- consultation is pushed to medical dossier
|
+-- 1.3 Lab workflow
|   |
|   +-- doctor requests lab test
|   +-- surgeon can also request lab test
|   +-- lab agent receives request
|   +-- lab agent uploads result
|   +-- result is pushed to medical dossier
|   +-- doctor/surgeon get notification
|
+-- 1.4 Prescription workflow
|   |
|   +-- doctor creates prescription
|   +-- pharmacy receives prescription
|   +-- pharmacist dispenses medication
|   +-- stock decrements
|   +-- dossier receives medication event if needed
|
+-- 1.5 Medical dossier
    |
    +-- consultation entries
    +-- lab result entries
    +-- hospitalization entries
    +-- surgery entries
    +-- discharge entries
```

### Pourquoi le dossier medical doit etre fait ici

Le dossier medical ne doit pas etre ajoute a la fin comme simple page frontend. Il doit etre alimente automatiquement par les workflows.

Donc il doit commencer en meme temps que:

- consultations
- lab results
- hospitalization
- procedure

Sinon chaque equipe va inventer son propre format de rapport.

### Dependances fortes

```text
clinical-service <-> procedure-service
clinical-service <-> ops-service
clinical-service <-> pharmacy-service
clinical-service <-> communication-service
```

### Ordre conseille

1. Appointments et consultations.
2. Lab requests/results.
3. Prescription vers pharmacy.
4. Medical dossier timeline minimal.
5. Notifications clinical.

## 5. Step 2: hospitalization and nurse operations

### Pourquoi grouper hospitalization avec nurse

Une hospitalisation n'est pas seulement une decision doctor. Elle implique:

- doctor
- receptionist
- nurse
- room/bed
- lab si necessaire
- pharmacy si materiel ou medicaments
- discharge/follow-up

Donc `ops-service` doit etre implemente avec `clinical-service`, `pharmacy-service` et `communication-service`.

### Microservices concernes

```text
ops-service
clinical-service
pharmacy-service
communication-service
user-service
FrontEnd
```

### Arbre logique

```text
2. Hospitalization and nurse operations
|
+-- 2.1 Doctor hospitalization decision
|   |
|   +-- from consultation
|   +-- reason
|   +-- urgency
|   +-- initial medical instructions
|
+-- 2.2 Receptionist admission
|   |
|   +-- assign room
|   +-- assign bed
|   +-- admission date
|   +-- patient administrative status
|
+-- 2.3 Nurse todolist
|   |
|   +-- tasks created from doctor instructions
|   +-- tasks created from surgeon pre-op/post-op
|   +-- execution status
|   +-- nurse note
|   +-- signature or validation
|
+-- 2.4 Pharmacy link
|   |
|   +-- nurse needs equipment
|   +-- nurse needs dialysis material
|   +-- nurse needs blood
|   +-- pharmacist records issue
|   +-- stock decrements automatically
|
+-- 2.5 Lab link
|   |
|   +-- nurse/lab sample flow if needed
|   +-- lab result visible to doctor/surgeon
|
+-- 2.6 Discharge and follow-up
    |
    +-- discharge summary
    +-- follow-up instructions
    +-- home supervision alerts
    +-- guardian visibility/print
```

### Taches a faire ensemble

- Hospitalization room/bed + receptionist admission.
- Nurse todolist + nurse dashboard.
- Nurse task result + dossier medical.
- Nurse equipment need + pharmacy issue.
- Discharge document + dossier medical + guardian print.

### Risque si separe

Si nurse todolist est faite sans pharmacy:

- le nurse peut declarer avoir utilise du materiel, mais le stock ne bouge pas.

Si hospitalization est faite sans dossier medical:

- les informations de sejour ne seront pas visibles au chirurgien ou au doctor plus tard.

## 6. Step 3: pharmacy and stock workflows

### Pourquoi pharmacy doit etre connecte au reste

La pharmacie ne doit pas etre seulement une page CRUD de stock. Elle est connectee a trois flux:

- prescription doctor -> pharmacist
- nurse material pickup -> pharmacist
- supplier delivery -> stock

### Microservices concernes

```text
pharmacy-service
clinical-service
ops-service
procedure-service
user-service
communication-service
FrontEnd
```

### Arbre logique

```text
3. Pharmacy and stock
|
+-- 3.1 Medication stock
|   |
|   +-- prescription received from doctor
|   +-- pharmacist validates/dispenses
|   +-- medication stock decrements
|   +-- patient/dossier can receive medication trace
|
+-- 3.2 Equipment stock
|   |
|   +-- nurse takes equipment
|   +-- pharmacist records nurse identity
|   +-- stock decrements
|   +-- ledger entry created
|
+-- 3.3 Dialysis and blood stock
|   |
|   +-- nurse takes dialysis material
|   +-- nurse takes blood if needed
|   +-- link to dialysis session/patient
|   +-- pharmacist records issue
|   +-- stock decrements
|
+-- 3.4 Supplier workflow
|   |
|   +-- order requested quantity
|   +-- supplier delivers partial/full quantity
|   +-- delivered quantity updates stock
|   +-- alert if low stock
|
+-- 3.5 Ledger/audit
    |
    +-- every stock change recorded
    +-- actor pharmacist/supplier/admin
    +-- before quantity
    +-- after quantity
    +-- reason
```

### Dependances fortes

```text
pharmacy-service <-> clinical-service
Reason: prescriptions.

pharmacy-service <-> ops-service
Reason: hospitalization, dialysis material, nurse execution.

pharmacy-service <-> procedure-service
Reason: pre-op/post-op material needs.

pharmacy-service <-> user-service
Reason: pharmacist and nurse identity must be real users, not free text.

pharmacy-service <-> communication-service
Reason: stock alerts and request notifications.
```

### Ordre conseille

1. Fix stock movement correctness.
2. Add pharmacist/nurse identity.
3. Add ledger.
4. Link to prescription.
5. Link to hospitalization/dialysis session.
6. Add supplier multi-stock delivery.
7. Add alerts.

## 7. Step 4: procedure and surgery workflows

### Pourquoi procedure doit etre faite avec clinical et ops

La chirurgie depend directement de:

- consultation doctor
- dossier medical
- lab tests
- nurse pre-op tasks
- receptionist scheduling
- operation room
- post-op hospitalization/follow-up

Donc `procedure-service` seul ne suffit pas.

### Microservices concernes

```text
procedure-service
clinical-service
ops-service
administration-service
communication-service
user-service
FrontEnd
```

### Arbre logique

```text
4. Procedure and surgery
|
+-- 4.1 Doctor referral
|   |
|   +-- doctor decides operation
|   +-- desired operation window
|   +-- reason and clinical note
|   +-- attach consultation/dossier context
|
+-- 4.2 Surgeon selection mode
|   |
|   +-- send to all surgeons
|   |   |
|   |   +-- FIFO
|   |   +-- first surgeon takes
|   |   +-- request disappears from others
|   |
|   +-- send to specific surgeon
|       |
|       +-- doctor sees availability
|       +-- doctor selects surgeon by name
|       +-- surgeon has expiration duration
|       +-- if not accepted, broadcast to all
|
+-- 4.3 Pre-operation workflow
|   |
|   +-- surgeon reads medical dossier
|   +-- surgeon creates pre-op nurse todolist
|   +-- surgeon requests lab tests if needed
|   +-- nurse completes todolist
|   +-- lab agent uploads results
|   +-- surgeon receives pre-op report
|   +-- surgeon marks patient ready/not ready
|
+-- 4.4 Receptionist scheduling
|   |
|   +-- surgeon sends planning request
|   +-- receptionist selects date
|   +-- receptionist selects time
|   +-- receptionist selects operating room
|   +-- surgeon/patient dashboards updated
|
+-- 4.5 Operation execution
|   |
|   +-- surgeon checkin
|   +-- operation in progress
|   +-- surgeon checkout
|   +-- availability recalculated
|
+-- 4.6 Post-operation
    |
    +-- surgeon writes first post-op report
    +-- report pushed to medical dossier
    +-- surgeon creates follow-up todolist
    +-- nurse executes follow-up
    +-- results pushed to medical dossier
    +-- hospitalization if needed
```

### Dependances fortes

```text
procedure-service <-> clinical-service
Reason: consultation source, lab requests, medical dossier.

procedure-service <-> ops-service
Reason: pre-op nurse todolist, post-op follow-up, hospitalization.

procedure-service <-> administration-service
Reason: operating room/resources.

procedure-service <-> user-service
Reason: doctor/surgeon/receptionist identities and roles.

procedure-service <-> communication-service
Reason: offers, expiration, notifications.
```

### Ordre conseille

1. Doctor referral request.
2. Surgeon offer/take model.
3. FIFO and specific-surgeon expiration.
4. Medical dossier read-only view for surgeon.
5. Pre-op nurse todolist and lab request.
6. Receptionist scheduling.
7. Checkin/checkout availability.
8. Post-op report and follow-up.

## 8. Step 5: communication layer

### Pourquoi communication est un bloc transversal

Messaging interne est deja une fonctionnalite, mais les workflows ont aussi besoin de notifications automatiques.

Exemples:

- lab result uploaded -> doctor/surgeon notified
- surgery request created -> surgeon notified
- specific surgeon expired -> all surgeons notified
- nurse task assigned -> nurse notified
- stock low -> pharmacist/admin notified
- operation schedule confirmed -> surgeon/doctor/receptionist notified

### Microservices concernes

```text
communication-service
Tous les autres microservices
FrontEnd
```

### Arbre logique

```text
5. Communication
|
+-- 5.1 Staff messaging
|   |
|   +-- conversations
|   +-- participants
|   +-- messages
|   +-- WebSocket updates
|
+-- 5.2 Workflow notifications
|   |
|   +-- event type
|   +-- target role
|   +-- target user
|   +-- payload entity
|   +-- read/unread status
|
+-- 5.3 Dashboard counters
    |
    +-- pending lab requests
    +-- pending surgery offers
    +-- nurse tasks today
    +-- low stock alerts
    +-- receptionist scheduling requests
```

### Ordre conseille

1. Keep staff messaging as independent feature.
2. Add generic workflow notification model.
3. Connect lab/procedure/pharmacy/hospitalization.
4. Add dashboard counters.

## 9. Step 6: administration, HR and public content

### Pourquoi ce bloc peut etre separe

Ces taches sont importantes, mais elles sont moins bloquees par les workflows cliniques.

Elles peuvent avancer en parallele si une personne ne travaille pas sur clinical/procedure/pharmacy.

### Microservices concernes

```text
administration-service
user-service
FrontEnd
```

### Arbre logique

```text
6. Administration and public content
|
+-- 6.1 Editable public home page
|   |
|   +-- admin CMS fields
|   +-- hero section
|   +-- services section
|   +-- doctors/clinic info
|   +-- public frontend rendering
|
+-- 6.2 HR/Admin staff supervision
|   |
|   +-- contracts
|   +-- staff performance stats
|   +-- attendance/availability if required
|
+-- 6.3 Resources
|   |
|   +-- clinic resources
|   +-- 2D/3D resources
|   +-- operating rooms if used by procedure
|
+-- 6.4 Statistics
    |
    +-- admin dashboard
    +-- HR dashboard
    +-- staff performance
    +-- service activity
```

### Important dependency

Operating rooms/resources must connect to `procedure-service`.

So:

- public home page can be independent
- contracts can be independent
- operating rooms/resources must be coordinated with procedure scheduling

## 10. Step 7: guardian and patient-facing features

### Pourquoi ce bloc vient apres clinical/hospitalization

Guardian features depend on documents already generated by clinical and ops workflows.

If discharge summary, lab result, hospitalization follow-up and procedure reports are not stable, guardian printing/upload visibility will be incomplete.

### Microservices concernes

```text
clinical-service
ops-service
procedure-service
communication-service
user-service
FrontEnd
```

### Arbre logique

```text
7. Guardian/patient-facing
|
+-- 7.1 Guardian uploads
|   |
|   +-- upload document
|   +-- link to patient
|   +-- staff review if needed
|
+-- 7.2 Guardian printing
|   |
|   +-- appointment documents
|   +-- discharge summary
|   +-- lab result if allowed
|   +-- follow-up instructions
|
+-- 7.3 Home supervision
|   |
|   +-- follow-up reminders
|   +-- medication reminders
|   +-- warning alerts
|   +-- doctor/nurse notification if critical
```

### Ordre conseille

1. Finish discharge/follow-up document.
2. Define guardian permissions.
3. Add printing.
4. Add uploads.
5. Add home supervision reminders.

## 11. Recommended implementation waves

### Wave 1: Foundation and clinical base

```text
Wave 1
|
+-- user-service
|   +-- roles
|   +-- 2FA if required
|   +-- staff identity
|
+-- communication-service
|   +-- workflow notifications
|
+-- clinical-service
|   +-- appointment correctness
|   +-- consultation close
|   +-- lab request/result
|
+-- FrontEnd
    +-- sidebar by role
    +-- settings/profile
```

Outcome:

- stable identity
- stable permissions
- clinical workflow can generate events
- other microservices can trust user IDs and notifications

### Wave 2: Medical dossier and hospital operations

```text
Wave 2
|
+-- clinical-service or medical-record-service
|   +-- dossier timeline
|   +-- consultation entries
|   +-- lab entries
|
+-- ops-service
|   +-- hospitalization
|   +-- room/bed
|   +-- nurse todolist
|   +-- discharge/follow-up
|
+-- FrontEnd
    +-- doctor dossier view
    +-- nurse dashboard
    +-- receptionist admission
```

Outcome:

- patient history starts becoming central
- nurses have a reliable workflow
- hospitalization data becomes reusable by procedure

### Wave 3: Pharmacy integration

```text
Wave 3
|
+-- pharmacy-service
|   +-- stock movement validation
|   +-- nurse/pharmacist identity
|   +-- ledger
|   +-- supplier delivery
|
+-- clinical-service
|   +-- prescription to pharmacy
|
+-- ops-service
|   +-- nurse material pickup
|   +-- dialysis/blood link
|
+-- FrontEnd
    +-- pharmacist dashboard
    +-- movement form
    +-- stock alerts
```

Outcome:

- stock becomes trustworthy
- pharmacy no longer acts as isolated CRUD
- nurse material usage is traceable

### Wave 4: Procedure/chirurgie

```text
Wave 4
|
+-- procedure-service
|   +-- referral request
|   +-- surgeon offers
|   +-- FIFO take
|   +-- specific surgeon expiration
|   +-- pre-op workflow
|   +-- checkin/checkout
|   +-- post-op report
|
+-- clinical-service
|   +-- dossier read
|   +-- lab request by surgeon
|
+-- ops-service
|   +-- pre-op nurse todolist
|   +-- post-op follow-up todolist
|
+-- administration-service
|   +-- operating rooms/resources
|
+-- FrontEnd
    +-- doctor surgery referral
    +-- surgeon dashboard
    +-- receptionist operation scheduling
```

Outcome:

- procedure becomes a real workflow, not only case CRUD
- surgeon availability becomes meaningful
- pre/post operation data enters dossier medical

### Wave 5: Admin, public and guardian polish

```text
Wave 5
|
+-- administration-service
|   +-- editable home page
|   +-- resources 2D/3D
|   +-- HR dashboards
|
+-- FrontEnd
|   +-- public home rendering
|   +-- guardian dashboard
|   +-- print/export
|
+-- communication-service
    +-- home follow-up reminders
```

Outcome:

- public/admin polish
- guardian workflows usable
- dashboards more complete

## 12. Best 3-person split by dependency

### Person A: Clinical core and dossier

Owns:

```text
clinical-service
medical dossier
lab workflow
consultation events
prescription event publishing
doctor frontend screens
```

Must coordinate with:

- Person B for nurse/hospitalization data.
- Person C for surgery dossier and lab needs.

### Person B: Ops, pharmacy and nurse workflows

Owns:

```text
ops-service
pharmacy-service
nurse dashboard
hospitalization
room/bed
stock movement
ledger
supplier delivery
```

Must coordinate with:

- Person A for prescriptions and dossier entries.
- Person C for pre-op/post-op nurse tasks.

### Person C: Procedure, receptionist scheduling and communication glue

Owns:

```text
procedure-service
surgery referral
surgeon offers
FIFO
specific surgeon expiration
operation scheduling
checkin/checkout
post-op reports
procedure frontend
workflow notifications
```

Must coordinate with:

- Person A for clinical dossier and lab requests.
- Person B for nurse tasks and post-op hospitalization/follow-up.

## 13. 3-day condensed plan

### Day 1: Models and contracts

```text
Day 1
|
+-- Person A
|   +-- medical dossier model
|   +-- consultation -> dossier
|   +-- lab result -> dossier
|
+-- Person B
|   +-- pharmacy movement validation
|   +-- nurse/pharmacist identity fields
|   +-- stock ledger model
|
+-- Person C
    +-- surgery referral model
    +-- surgery offer model
    +-- FIFO accept endpoint
```

### Day 2: Workflow integration

```text
Day 2
|
+-- Person A
|   +-- prescription event to pharmacy
|   +-- surgeon lab request support
|   +-- doctor/surgeon dossier read API
|
+-- Person B
|   +-- nurse material pickup workflow
|   +-- stock decrement + ledger transaction
|   +-- supplier delivery for all stock types
|
+-- Person C
    +-- specific surgeon expiration
    +-- surgeon availability
    +-- pre-op nurse todolist integration
```

### Day 3: Frontend and demo path

```text
Day 3
|
+-- Person A
|   +-- dossier UI
|   +-- lab result visibility
|   +-- appointment 15 min correction
|
+-- Person B
|   +-- pharmacist dashboard
|   +-- movement/ledger UI
|   +-- nurse material request UI
|
+-- Person C
    +-- surgeon dashboard
    +-- receptionist operation scheduling UI
    +-- checkin/checkout UI
    +-- post-op report UI
```

## 14. Critical dependency map

```text
Clinical consultation
|
+-- creates dossier entry
+-- can create prescription
|   |
|   +-- pharmacy stock workflow
|
+-- can create hospitalization
|   |
|   +-- ops nurse workflow
|   +-- pharmacy material workflow
|   +-- discharge/follow-up
|
+-- can create surgery referral
    |
    +-- procedure surgeon offers
    +-- surgeon reads dossier
    +-- surgeon requests lab
    +-- pre-op nurse tasks
    +-- receptionist schedules operation
    +-- checkin/checkout availability
    +-- post-op report to dossier
    +-- post-op nurse follow-up
```

## 15. What should not be implemented alone

Do not implement these in isolation:

- Procedure without clinical dossier.
- Procedure without ops nurse todolist.
- Pharmacy stock movement without user-service identity.
- Pharmacy dialysis/blood without ops/hospitalization/dialysis context.
- Guardian printing before discharge/follow-up documents are stable.
- Admin operating rooms without procedure scheduling.
- Notifications after the workflows are finished; they should be integrated while building the workflows.

## 16. Recommended priority order

```text
1. Auth/roles/identity/notifications foundation
2. Clinical appointment/consultation/lab/prescription
3. Medical dossier common timeline
4. Hospitalization + nurse todolist + discharge
5. Pharmacy stock movement + ledger + supplier delivery
6. Procedure referral + FIFO + surgeon availability
7. Pre-op/post-op workflow integration
8. Receptionist scheduling for operation
9. Guardian print/upload/home follow-up
10. Admin public home/resources/statistics polish
```

## 17. Final recommendation

The best architecture is not to organize the remaining work by microservice names only. Organize it by patient journey:

```text
Appointment
-> Consultation
-> Lab/Prescription/Hospitalization/Procedure decision
-> Nurse/Pharmacy/Receptionist execution
-> Dossier medical update
-> Discharge/Follow-up/Guardian visibility
```

This keeps the project coherent and prevents each microservice from becoming a separate CRUD application with no real hospital workflow.
