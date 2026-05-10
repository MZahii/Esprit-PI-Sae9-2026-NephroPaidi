# Analyse todolist totale, microservices et workflows metier

Date d'analyse: 2026-05-08  
Projet: NephroPaidi / Esprit-PI-Sae9-2026  
Portee: backend microservices, frontend Angular, todolist totale, workflow pharmacie, workflow procedure/chirurgie, documents metier fournis.

## 1. Sources analysees

### Sources projet

- `todolist-TOTALE.docx`
- `docs/HOSPITALIZATION_WORKFLOW_CHANGES.md`
- Backend: `BackEnd/microservices/*`
- Frontend: `FrontEnd/src/app`

### Documents metier fournis

- `FICHE-CRH-1.pdf`
- `Prot_001.pdf`
- `LLS commentee.pdf`
- `document_de_sortie_contenu_metier_23102014.pdf`
- Recommandations SnephroPed: https://www.snephroped.org/ressources/recommandations/

Note: `LLS commentee.pdf` ne contient presque pas de texte extractible automatiquement. Il semble etre scanne ou image-only. Une OCR serait necessaire pour une analyse exhaustive de ce document.

## 2. Inventaire rapide des microservices

| Microservice | Etat observe | Commentaire |
| --- | --- | --- |
| `administration-service` | actif | Gestion administrative/contrats/ressources partiellement presente. |
| `user-service` | actif | Authentification, forgot password, profils basiques. 2FA absente. |
| `communication-service` | actif | Messagerie interne staff presente avec REST/WebSocket. |
| `clinical-service` | actif | Consultations, rendez-vous, lab requests, hospitalisation cote doctor. |
| `ops-service` | actif | Hospitalisations, chambre/lit, nurse tasks, lab inbox, admissions. |
| `pharmacy-service` | actif | Medicaments, equipements, dialysis stock, suppliers, movements. Plusieurs ecarts metier. |
| `procedure-service` | actif | Surgical cases, pre/post operation, complications, care tasks. Workflow demande non complet. |
| `ai-clinical-service` | squelette | Aucun controller significatif observe. |
| `ai-pharmacy-service` | squelette | Aucun controller significatif observe. |
| `core-ops-service` | squelette | Aucun controller significatif observe. |
| `egfr-ml-service` | service specialise | Hors workflow principal, probablement ML/eGFR. |

## 3. Resume executif

Le projet contient deja une base solide: authentification, forgot password, messagerie interne, dashboards, hospitalisation, lab inbox, pharmacie avec stocks, et une premiere base procedure/chirurgie.

Cependant, plusieurs implementations ne correspondent pas encore exactement a la todolist totale ni aux workflows precises par le besoin. Les deux zones les plus critiques sont:

- Pharmacie: la sortie des equipements/dialysis/blood n'est pas assez encadree par une vraie demande nurse -> pharmacien -> deduction stock tracee. Le stock peut meme enregistrer un mouvement sans decrement si la ligne de stock cible n'existe pas.
- Procedure/chirurgie: le workflow demande autour des chirurgiens, FIFO, surgeon specifique avec expiration, pre-operation, planning par receptionist, checkin/checkout et dossier medical commun n'est implemente que tres partiellement.

Le manque transversal le plus important est le dossier medical commun. Aujourd'hui, les consultations, lab tests, hospitalisations, chirurgie et rapports restent disperses entre microservices. Le besoin demande une entite/metier commune qui regroupe automatiquement tous ces evenements.

## 4. Comparaison todolist totale vs implementation

| Exigence todolist | Etat | Analyse |
| --- | --- | --- |
| Home page publique modifiable par admin | Manquant | Le frontend contient une home page hardcodee. Pas de CMS admin ni backend de contenu. |
| Login remember me | Fait | Present dans `login.ts` et `login.html`. |
| Forgot password | Fait | Endpoint backend present dans `AuthController`. |
| 2FA | Manquant | Pas de flow OTP/TOTP/email verification observe. |
| Messaging interne staff | Fait | Service communication + route frontend + sidebar. |
| Sidebar gauche amelioree | Partiel | Sidebar existe, mais une verification UX detaillee reste a faire. |
| Statistiques dynamiques admin/HR/performance | Partiel | Certaines stats existent, mais pas une supervision complete staff/performance. |
| Audit logs et guidance | Partiel | Quelques traces existent, mais pas d'audit transversal robuste. |
| Ressources clinique 2D/3D | Partiel/indetermine | Microservice administration a des ressources, mais pas de validation complete 2D/3D. |
| Notifications role-based partout | Partiel | Notifications existent dans certains workflows, pas une architecture globale. |
| Settings profile/security/language | Partiel | Profil existe. Security/language incomplets. |
| HR/Admin contrats/supervision | Partiel | Contrats visibles, mais pas tous les scenarios RH. |
| Receptionist dashboard | Partiel | Plusieurs elements existent, mais besoin de consolidation planning/lab/procedure. |
| Appointment reschedule selon disponibilite | Partiel | A verifier/renforcer. |
| Auto-cancel appointment apres 15 min | Non conforme | Code observe avec 20 min et 30 min selon endroits. Exigence: 15 min. |
| Doctor dashboard today appointments | Fait/partiel | Route et composants presents. |
| Doctor peut start seulement rendez-vous du jour | Fait/partiel | Controle present, mais incoherences temps a corriger. |
| Hospitalization todo/signature/room-bed | Partiel | Chambre/lit et nurse workflow presents. Signature manuelle indiquee comme absente dans la doc. |
| Doctor appointment request to receptionist | Partiel | Present partiellement, a verifier bout-en-bout. |
| Doctor lab requests | Fait/partiel | Lab request existe. |
| Receptionist voit seulement infos minimales lab | Partiel | A verifier finement cote DTO/roles. |
| Prescription vers pharmacy | Partiel | Des liens existent, mais pas un workflow complet trace prescription -> dispensation. |
| Nurse dashboard/todolist/signature | Partiel | Tasks presentes; signature complete non confirmee. |
| Lab agent dashboard/uploads | Fait/partiel | Lab inbox/upload presents. |
| Pharmacy pharmacist-only | Non conforme | Frontend autorise `PHARMACIST`, `ADMIN`, `NURSE`, et route publique frontoffice pharmacy observee. |
| Pharmacy 3 stocks | Fait | Medicaments, equipment, dialysis stock existent. Blood stock n'est pas clairement separe. |
| Pharmacy alerts/suppliers | Partiel | Suppliers/orders existent. Livraison semble surtout incrementer medication. |
| Supplier requested vs delivered | Fait/partiel | Champs ordered/delivered presents. Effets stock incomplets selon type. |
| Outgoing equipment/dialysis auto-deduct | Partiel/non robuste | Movement existe et deduct si stock trouve. Mais mouvement peut etre sauve sans decrement si stock absent. |
| Surgery/procedure workflow | Tres partiel | Base procedure presente, mais FIFO, targeted surgeon, expiration, availability, pre-op nurse todo, receptionist scheduling manquent. |
| Guardian uploads | Indetermine/partiel | Besoin d'une verification dediee. |
| Dialysis session documentation/timer | Indetermine/partiel | Des stocks dialysis existent, mais session dialysis metier pas clairement complete. |
| Guardian printing | Indetermine/partiel | Non confirme. |
| Discharge follow-up + home supervision alerts/reminders | Partiel/manquant | Hospitalisation existe, mais sortie et suivi domicile doivent etre formalises. |

## 5. Analyse workflow pharmacie

### 5.1 Ce qui est deja implemente

- Trois familles de stock sont presentes cote backend:
  - medication stock
  - equipment stock
  - dialysis stock
- Les mouvements de stock existent via `StockMovement`.
- Les mouvements contiennent `stockType`, `itemId`, `itemName`, `quantityTaken`, `requestedBy`, `requestedByRole`, `purpose`, `takenAt`.
- Le frontend pharmacie a des ecrans equipment/dialysis et un historique movements.
- Les suppliers/orders existent avec quantite commandee et quantite livree.
- Le backend tente de decrementer le stock lors d'un mouvement equipment/dialysis.

### 5.2 Ecarts avec le workflow demande

Workflow demande:

> A chaque fois que le nurse prend des equipements medicaux ou blood pour la dialyse, le pharmaciste enregistre cette sortie avec le nom du nurse, et au meme temps ces equipements ou blood decrementent automatiquement du stock.

Ecarts constates:

- Le systeme ne force pas que la personne ayant pris le materiel soit une nurse.
- `requestedBy` est une chaine de texte, pas une reference forte vers un utilisateur/nurse.
- `requestedByRole` est trop libre et le frontend propose plusieurs roles: `DOCTOR`, `NURSE`, `PHARMACIST`, `ADMIN`.
- Il n'y a pas de champ clair `dispensedByPharmacistId` pour tracer le pharmacien qui enregistre.
- Il n'y a pas de relation obligatoire avec:
  - patient
  - hospitalisation
  - dialysis session
  - procedure
  - prescription ou ordre medical
- Le champ `purpose` est libre, donc difficile a auditer.
- Le stock blood n'est pas clairement modelise comme stock separe. Le besoin mentionne blood pour dialyse, mais l'implementation visible parle surtout equipment/dialysis stock.
- Le workflow nurse -> pharmacien n'est pas formalise: il n'y a pas de demande, approbation, preparation, remise, annulation.

### 5.3 Problemes techniques/logiques

Probleme critique:

- Dans le service de mouvement stock, la deduction est faite seulement si une ligne stock existe. Si elle n'existe pas, le mouvement peut quand meme etre enregistre. Cela peut produire un historique disant "sortie effectuee" sans baisse reelle du stock.

Autres risques:

- DTO de mouvement sans validation stricte observee: quantite positive, type obligatoire, item existant, requester obligatoire.
- Les endpoints d'ajustement direct permettent potentiellement de modifier le stock sans ledger metier complet.
- La livraison supplier semble incrementer clairement le stock medicament, mais pas necessairement equipment/dialysis.
- Les droits frontend/backend ne sont pas alignes avec "pharmacist-only".
- Une route publique/frontoffice pharmacie existe, ce qui est dangereux si elle expose des donnees sensibles ou donne une mauvaise surface fonctionnelle.

### 5.4 Implementation cible recommandee

Creer un workflow explicite de sortie stock:

1. `MedicalStockRequest`
   - `id`
   - `requestedByNurseId`
   - `requestedByNurseNameSnapshot`
   - `patientId`
   - `dialysisSessionId` optionnel mais recommande pour dialyse
   - `hospitalizationId` optionnel
   - `purposeType`: `DIALYSIS`, `HOSPITALIZATION`, `PROCEDURE`, `EMERGENCY`
   - `status`: `DRAFT`, `SUBMITTED`, `APPROVED`, `DISPENSED`, `REJECTED`, `CANCELLED`
   - `createdAt`, `updatedAt`

2. `MedicalStockRequestLine`
   - `requestId`
   - `stockType`: `EQUIPMENT`, `DIALYSIS`, `BLOOD`, `MEDICATION`
   - `itemId`
   - `requestedQuantity`
   - `approvedQuantity`

3. `StockIssue`
   - `requestId`
   - `dispensedByPharmacistId`
   - `dispensedAt`
   - `receivedByNurseId`
   - `signature` ou preuve de reception si le projet garde la logique signature

4. `StockLedgerEntry`
   - immutable
   - `sourceType`: `REQUEST`, `SUPPLIER_DELIVERY`, `MANUAL_ADJUSTMENT`, `RETURN`, `WASTE`
   - `sourceId`
   - `delta`
   - `beforeQty`
   - `afterQty`
   - `performedBy`
   - `reason`

Regle transactionnelle obligatoire:

- verifier item existe
- verifier stock suffisant
- creer issue
- decrementer stock
- creer ledger
- notifier nurse/doctor si besoin
- tout dans une transaction atomique

## 6. Analyse workflow procedure/chirurgie

### 6.1 Workflow demande

Le workflow demande peut etre resume ainsi:

1. Le docteur decide que le patient doit faire une operation dans une duree identifiee.
2. Le docteur peut:
   - envoyer la request a tous les chirurgiens
   - ou choisir un chirurgien specifique recommande par nom
3. Si la request est envoyee a tous:
   - FIFO
   - le premier chirurgien qui prend l'operation la gagne
   - elle disparait du dashboard des autres chirurgiens
4. Si elle est envoyee a un chirurgien specifique:
   - il doit accepter dans une duree specifique
   - s'il n'accepte pas, la request est envoyee automatiquement a tous
5. Quand un chirurgien prend l'operation:
   - workflow pre-operation
   - workflow operation checkin/checkout
   - workflow post-operation
6. Le chirurgien doit lire le dossier medical commun du patient.
7. Le dossier medical commun doit contenir automatiquement:
   - consultations doctor avec date et resultats
   - lab tests
   - rapports pre-operation
   - rapports post-operation
8. Le chirurgien envoie une todolist pre-operation au nurse.
9. Il peut demander des lab tests comme le docteur.
10. Quand la todolist et les lab tests sont finis, le chirurgien recoit un rapport.
11. Si le patient est pret, le chirurgien demande au receptionist de planifier date, heure, salle.
12. Checkin au debut de l'operation et checkout a la fin.
13. Le checkin/checkout definit la disponibilite du chirurgien.
14. Apres operation:
   - premier post-rapport chirurgical
   - todolist de follow-up/hospitalisation pour nurses
   - resultats enregistres dans le dossier medical.

### 6.2 Ce qui existe deja

- `procedure-service` contient:
  - surgery requests
  - surgical cases
  - pre-op assessment
  - post-op observation
  - complications
  - care tasks
  - PDF/reporting partiel
- Le frontend contient des pages:
  - procedure surgical
  - procedure surgical advanced
- Des champs existent pour patient, chirurgien, salle, dates, statut, notes cliniques.
- Les pre/post operations existent sous forme de formulaires ou notes.

### 6.3 Ecarts majeurs

#### Request doctor -> surgeon

Manquant:

- Pas de mode cible clair: `ALL_SURGEONS` vs `SPECIFIC_SURGEON`.
- Pas de table d'offres par chirurgien.
- Pas de FIFO atomique.
- Pas d'endpoint `take` securise qui garantit que deux chirurgiens ne peuvent pas prendre la meme operation.
- Pas d'expiration automatique pour chirurgien specifique.
- Pas de re-broadcast automatique vers tous les chirurgiens apres expiration.
- Pas de recommandation chirurgien par nom avec disponibilite.

#### Disponibilite chirurgien

Manquant:

- Checkin/checkout operationnel avec timestamps reels.
- Calcul de disponibilite depuis:
  - operations planifiees
  - operation en cours
  - checkin sans checkout
  - absences ou indisponibilites
- Affichage clair au docteur avant choix du chirurgien.

#### Pre-operation

Partiel/manquant:

- Le chirurgien ne lit pas un vrai dossier medical commun consolide.
- Les consultations doctor ne sont pas automatiquement agregees dans une entite dossier medical commune.
- Les lab tests ne sont pas automatiquement centralises dans ce dossier commun.
- La todolist pre-operation nurse n'est pas un workflow nurse complet partage avec ops-service.
- Le resultat de la todolist n'est pas automatiquement transforme en rapport structure pour le chirurgien.
- Les demandes lab par chirurgien ne sont pas clairement integrees au workflow lab existant.

#### Planning par receptionist

Non conforme:

- L'existant permet de creer/planifier des surgical cases directement.
- Le besoin demande une etape apres validation pre-op: le chirurgien demande au receptionist de planifier date, heure et salle.
- Il manque donc une entite `OperationScheduleRequest` et un dashboard receptionist associe.

#### Post-operation

Partiel:

- Des post-op observations existent.
- Mais il manque un rapport post-operation structure comme document metier.
- Il manque le lien automatique vers dossier medical.
- Il manque le follow-up nurse/hospitalisation formalise apres operation.

### 6.4 Implementation cible recommandee

Entites recommandees:

- `SurgeryReferralRequest`
  - creee par doctor
  - patient, consultation, reason, urgency, desiredWindowStart/End
  - targetMode: `ALL_SURGEONS` ou `SPECIFIC_SURGEON`
  - targetSurgeonId si specifique
  - expiresAt si specifique
  - status: `PENDING`, `OFFERED`, `ACCEPTED`, `EXPIRED`, `BROADCASTED`, `CANCELLED`

- `SurgeryOffer`
  - requestId
  - surgeonId
  - status: `AVAILABLE`, `ACCEPTED`, `EXPIRED`, `WITHDRAWN`
  - offeredAt, expiresAt, acceptedAt

- `OperationScheduleRequest`
  - surgicalCaseId
  - requestedBySurgeonId
  - patientId
  - preferredDateRange
  - requiredRoomType
  - status: `PENDING_RECEPTIONIST`, `SCHEDULED`, `REJECTED`

- `OperationExecution`
  - surgicalCaseId
  - checkinAt
  - checkoutAt
  - checkedInBy
  - checkedOutBy
  - status: `READY`, `IN_PROGRESS`, `COMPLETED`

- `SurgeryReport`
  - surgicalCaseId
  - reportType: `PRE_OPERATION`, `POST_OPERATION`, `FOLLOW_UP`
  - structuredContent
  - authorSurgeonId
  - signedAt
  - automaticallyPublishedToDossier

Regles importantes:

- `takeOperation(requestId, surgeonId)` doit etre transactionnel avec lock pessimiste ou contrainte unique.
- Si request ALL: premier `ACCEPTED` gagne, les autres offres passent `WITHDRAWN`.
- Si request SPECIFIC expire: scheduler passe l'offre en `EXPIRED` et cree des offres pour tous les chirurgiens disponibles.
- La disponibilite chirurgien doit etre calculee depuis les operations `SCHEDULED` et `IN_PROGRESS`.

## 7. Dossier medical commun

### 7.1 Probleme actuel

Les informations patient sont dispersees:

- consultations dans `clinical-service`
- lab tests dans clinical/ops selon workflow
- hospitalisation dans `ops-service`
- chirurgie dans `procedure-service`
- documents de sortie non consolides
- pharmacie separee

Le besoin demande une seule entite commune entre doctor et surgeon.

### 7.2 Dossier cible

Creer un dossier medical transverse, soit dans `clinical-service`, soit dans un nouveau `medical-record-service`.

Sections minimales:

- Identite patient
- Antecedents, allergies, pathologies chroniques
- Timeline consultations
- Timeline lab tests
- Timeline hospitalisations
- Timeline procedures/chirurgies
- Rapports pre-operation
- Rapports post-operation
- Documents de sortie
- Attachments/uploads
- Audit trail

Chaque evenement doit etre publie automatiquement:

- consultation terminee -> entree dossier medical
- lab result upload -> entree dossier medical
- pre-op report signe -> entree dossier medical
- post-op report signe -> entree dossier medical
- discharge summary -> entree dossier medical

### 7.3 Alignement avec documents metier

Le document HAS de sortie hospitaliere impose une structure utile pour la sortie:

- motif d'hospitalisation
- synthese medicale du sejour
- actes techniques, examens, biologie
- traitements medicamenteux
- suite a donner
- identification patient
- dates entree/sortie
- service
- redacteur/signataire
- destination

`FICHE-CRH-1.pdf` confirme le besoin d'un compte rendu d'hospitalisation structure, avec informations administratives, contexte clinique et elements de suivi.

`Prot_001.pdf` confirme l'importance d'un recueil standardise en nephrologie pediatrique: historique medical, examen physique, constantes, oedemes, biologie, prelevements, qualite des donnees.

## 8. Implementations non identiques ou non logiques a noter

### Pharmacie

- La pharmacie n'est pas strictement pharmacist-only.
- La sortie de stock accepte trop de roles.
- Le nom du nurse est texte libre au lieu d'une reference utilisateur.
- Le pharmacien qui enregistre n'est pas trace proprement.
- Le stock peut ne pas decrementer si la ligne stock n'existe pas, tout en gardant le mouvement.
- Pas de blood stock clairement separe.
- Pas de lien avec dialysis session.
- Pas de workflow request/approval/dispense.
- Supplier delivery n'est pas completement coherente pour equipment/dialysis.

### Procedure

- Pas de FIFO.
- Pas de targeted surgeon avec expiration.
- Pas de rebroadcast automatique vers tous les chirurgiens.
- Pas de vraie disponibilite chirurgien basee sur checkin/checkout.
- Pas de dossier medical commun.
- Pre-op nurse todolist non integree a ops-service.
- Lab request par chirurgien non complete.
- Scheduling receptionist non respecte comme etape separee.
- Rapports pre/post operation pas assez structures.

### Hospitalisation

- Chambre/lit present.
- Signature manuelle absente selon la documentation interne.
- Le lien post-operation -> hospitalisation/follow-up nurse doit etre ajoute.

### Rendez-vous

- Regle auto-cancel non conforme: todolist dit 15 min, code observe 20/30 min.

### Auth

- Remember me et forgot password OK.
- 2FA manquante.

### Frontend public/admin

- Home page non editable par admin.
- Risque de routes trop ouvertes, notamment pharmacy.

## 9. Ameliorations prioritaires

### Priorite 1: securiser les workflows critiques

- Corriger pharmacie stock movement pour interdire mouvement sans stock trouve.
- Ajouter validation DTO stricte.
- Restreindre pharmacy a `PHARMACIST` cote frontend et backend, sauf cas volontaire documente.
- Ajouter `dispensedByPharmacistId` et `receivedByNurseId`.
- Implementer FIFO/take operation transactionnel.
- Ajouter expiration targeted surgeon.

### Priorite 2: aligner le metier

- Creer dossier medical commun.
- Publier automatiquement consultations/lab/pre-op/post-op dans dossier medical.
- Ajouter workflow pre-op todolist nurse.
- Ajouter workflow scheduling receptionist apres pre-op readiness.
- Ajouter checkin/checkout operation.

### Priorite 3: qualite et audit

- Ajouter ledger stock immutable.
- Ajouter audit trail transversal.
- Ajouter notifications role-based.
- Ajouter tests integration pour workflows metier.
- Harmoniser les statuts et enums entre frontend/backend.

## 10. Nouvelle todo list repartie entre 3 personnes sur 3 jours

Hypothese: 3 developpeurs full-stack disponibles, objectif de livrer une version coherente et demonstrable en 3 jours, pas une refonte parfaite.

### Personne 1: Pharmacie + stock + audit

#### Jour 1

- Auditer endpoints pharmacy existants.
- Corriger bug: aucun mouvement ne doit etre sauvegarde si le stock cible n'existe pas.
- Ajouter validation DTO: quantite positive, stockType obligatoire, itemId obligatoire, requester obligatoire.
- Restreindre routes pharmacy aux roles voulus.
- Ajouter champs backend:
  - `dispensedByPharmacistId`
  - `dispensedByPharmacistNameSnapshot`
  - `receivedByNurseId`
  - `receivedByNurseNameSnapshot`
  - `patientId`
  - `dialysisSessionId`
  - `purposeType`

#### Jour 2

- Implementer `StockLedgerEntry`.
- Toute entree/sortie stock doit creer une ligne ledger.
- Ajouter support clair de `BLOOD` si le besoin reste confirme.
- Corriger supplier delivery pour incrementer medication/equipment/dialysis/blood selon type.
- Adapter frontend pharmacy movement form pour nurse obligatoire et pharmacist courant automatique.

#### Jour 3

- Ajouter tests backend:
  - movement impossible si item inexistant
  - movement impossible si stock insuffisant
  - movement decrement correct
  - ledger cree
  - supplier delivery increment correct
- Ajouter ecran historique ledger.
- Documenter le workflow pharmacie.

Livrable attendu:

- Workflow pharmacie coherent: nurse identifie, pharmacien trace, stock decremente automatiquement, audit disponible.

### Personne 2: Procedure/chirurgie

#### Jour 1

- Ajouter modele request/offer:
  - `SurgeryReferralRequest`
  - `SurgeryOffer`
  - target mode all/specific
  - expiresAt
- Ajouter endpoint doctor create surgery referral.
- Ajouter endpoint surgeon list available offers.
- Ajouter endpoint transactionnel `take/accept`.

#### Jour 2

- Ajouter scheduler expiration targeted surgeon -> broadcast all.
- Ajouter disponibilite chirurgien simple:
  - available
  - scheduled
  - in_operation
- Ajouter checkin/checkout operation.
- Ajouter affichage frontend doctor: liste chirurgiens + disponibilite.
- Ajouter disparition de request des autres dashboards apres acceptation.

#### Jour 3

- Ajouter `OperationScheduleRequest` vers receptionist.
- Ajouter dashboard receptionist minimal pour planifier date/heure/salle.
- Ajouter pre-op readiness state.
- Ajouter post-op report structure minimal.
- Ajouter tests FIFO/expiration/checkin/checkout.

Livrable attendu:

- Workflow doctor -> surgeon all/specific -> accept FIFO/expiration -> pre-op ready -> receptionist schedule -> checkin/checkout -> post-op report.

### Personne 3: Dossier medical commun + integration frontend

#### Jour 1

- Definir modele `MedicalDossierEntry`.
- Creer API dossier patient:
  - timeline
  - filters consultation/lab/hospitalization/procedure/report
- Brancher consultation terminee -> dossier.
- Brancher lab result upload -> dossier.

#### Jour 2

- Brancher pre-op report -> dossier.
- Brancher post-op report -> dossier.
- Brancher discharge/hospitalization summary -> dossier.
- Creer page frontend dossier medical commune doctor/surgeon.

#### Jour 3

- Ajouter modele document de sortie/CRH minimal base sur HAS:
  - motif
  - synthese
  - actes/examens/biologie
  - traitements
  - suite a donner
  - auteur/date/service
- Ajouter export/print minimal si deja infrastructure PDF.
- Ajouter tests API dossier et verification droits doctor/surgeon.

Livrable attendu:

- Dossier medical commun consultable par doctor et surgeon, alimente automatiquement par les evenements principaux.

## 11. Planning global conseille

### Jour 1: Verrouiller les bases

- Pharmacy: validation + correction movement sans stock.
- Procedure: request/offer + accept transactionnel.
- Dossier: modele timeline + consultation/lab.

### Jour 2: Connecter les workflows

- Pharmacy: ledger + supplier multi-type.
- Procedure: expiration + availability + checkin/checkout.
- Dossier: pre/post/hospitalization integration.

### Jour 3: Rendre demonstrable

- Pharmacy: UI propre + tests.
- Procedure: receptionist scheduling + tests.
- Dossier: page commune + CRH/document sortie minimal.

## 12. Risques si on ne corrige pas

- Stock pharmacie faux: mouvements enregistres sans decrement reel.
- Audit impossible: on ne peut pas prouver quel nurse a pris quoi et quel pharmacien a donne quoi.
- Operation prise par plusieurs chirurgiens si FIFO non atomique.
- Chirurgien choisi alors qu'il est indisponible.
- Pre-op incomplet car lab/todolist nurse non rattaches.
- Dossier patient fragmente, donc doctor/surgeon travaillent avec une vision incomplete.
- Demo fonctionnelle fragile car les workflows existent en morceaux mais pas en processus metier complet.

## 13. Conclusion

Le projet est avance sur la structure generale, mais les workflows pharmacie et procedure doivent etre renforces pour devenir metierement fiables.

La meilleure strategie en 3 jours est de ne pas refaire tout le projet, mais de securiser les points critiques:

- stock decrement atomique et trace
- nurse/pharmacien identifies
- FIFO chirurgie transactionnel
- targeted surgeon avec expiration
- checkin/checkout et disponibilite
- dossier medical commun minimal mais automatique

Ces changements donneront une version beaucoup plus coherente, defendable en soutenance, et plus proche de la todolist totale.
