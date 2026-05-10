# AI Triage Deployment Log

## Control Rules

- Preserve the current guardian follow-up workflow unless a future phase is explicitly approved.
- Do not change receptionist, nurse, doctor, queue, routing, dashboard, or nurse-to-doctor escalation behavior during the audit phase.
- Keep `communication-service` as the owner of message workflow state.
- Keep the AI model as a support layer until enough validation exists to justify workflow automation.
- Document each phase before and after changes.

## Current Status

- Phase: 2C - Docker/config wiring for AI triage service.
- Workflow behavior changes: none; guardian message creation now stores advisory AI metadata when available.
- Files modified in this phase: Docker Compose/startup configuration only.
- Next recommended action: rerun runtime Docker validation after Docker Desktop recovers, then test one guardian message creation flow.

## Phase 0 - Architecture And Modeling Audit

### Objective

Understand the existing communication workflow and determine whether the current AI modeling artifacts can be safely deployed into NephroPaidi without changing existing workflow behavior.

### Analysis And Reasoning

The project is a microservices application. The communication workflow is not isolated in a notebook or a separate script; it is already implemented as a real application workflow in `communication-service` and surfaced in Angular backoffice/frontoffice screens.

The correct integration point is therefore not a public standalone prediction API used directly by the frontend. The correct integration point is behind `communication-service`, because that service already owns:

- guardian follow-up message creation;
- patient resolution for guardian messages;
- queue routing;
- staff inbox access;
- assignment and unassignment;
- mark-read and reply behavior;
- nurse-to-doctor escalation;
- audit logs.

The AI model should be introduced as AI-assisted triage metadata attached to a message. In the first deployment it should not take ownership of routing, assignment, escalation, or dashboards.

### Existing Architecture Summary

- `docker-compose.full.yml` orchestrates Keycloak, RabbitMQ, Eureka, Config Server, API Gateway, backend microservices, and the Angular frontend.
- `api-gateway` exposes the backend services to the frontend.
- `communication-service` is a Spring Boot service on port `8085`.
- `communication-service` is configured through `BackEnd/config-server/src/main/resources/config/communication-service.yml`.
- `communication-service` uses Keycloak JWT roles through `CurrentUserService`.
- The frontend communicates through `FrontEnd/src/app/core/services/communication-api.service.ts`.

### Current Communication Workflow Summary

1. Guardian sends a follow-up message through the Angular frontoffice communication page.
2. Frontend calls `POST /api/communication/messages`.
3. `CommunicationMessageController.create(...)` delegates to `FollowUpMessageService.create(...)`.
4. `FollowUpMessageServiceImpl.create(...)`:
   - requires `GUARDIAN`;
   - resolves the patient with `GuardianPatientResolverService`;
   - creates `FollowUpMessage`;
   - stores message type, guardian priority, subject, text, status, and queue;
   - applies existing `routeQueue(messageType, priority)`;
   - saves the message;
   - writes `CREATED` and `ROUTED` audit logs.
5. Staff inboxes call `GET /api/communication/backoffice/inbox`.
6. `staffInbox(...)` restricts staff to their own queue by deriving queue from the authenticated staff role.
7. Staff can take, unassign, mark read, reply, or close according to current access rules.
8. Nurse escalation is already implemented through `POST /api/communication/messages/{id}/escalate`.
9. Escalation currently moves a message to the doctor queue, assigns the selected doctor, marks status `ESCALATED`, and records the reason in audit details.

### Existing Roles And Queues

Current communication roles:

- `GUARDIAN`
- `RECEPTIONIST`
- `NURSE`
- `DOCTOR`

Current message queues:

- `RECEPTIONIST`
- `NURSE`
- `DOCTOR`

Current priority model:

- `NORMAL`
- `HIGH`

The AI model's urgency classes are different from the current priority model:

- `LOW`
- `MEDIUM`
- `HIGH`
- `CRITICAL`

Deployment implication: AI urgency should be represented separately from current `PriorityLevel`. It should not replace `PriorityLevel` without a separately approved workflow change.

### Files Inspected

Backend communication workflow:

- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/controller/CommunicationMessageController.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/controller/CommunicationBackofficeController.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/service/FollowUpMessageService.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/service/impl/FollowUpMessageServiceImpl.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/domain/entity/FollowUpMessage.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/domain/entity/MessageAuditLog.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/domain/enums/MessageQueue.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/domain/enums/MessageStatus.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/domain/enums/MessageType.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/domain/enums/PriorityLevel.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/domain/enums/StaffRole.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/domain/enums/MessageAuditAction.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/dto/request/CreateMessageRequest.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/dto/request/EscalateRequest.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/dto/request/InboxQueryParams.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/dto/response/CreateMessageResponse.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/dto/response/FollowUpMessageResponse.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/mapper/MessageMapper.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/repository/FollowUpMessageRepository.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/security/CurrentUserService.java`
- `BackEnd/microservices/communication-service/src/main/resources/db/migration`
- `BackEnd/microservices/communication-service/src/test/java/tn/esprit/spring/communicationservice/service/impl/FollowUpMessageServiceImplTest.java`
- `BackEnd/microservices/communication-service/README.md`
- `docs/COMMUNICATION_WORKFLOW_CHANGES.md`
- `docs/INTERNAL_STAFF_MESSAGING_CHANGES.md`

Frontend communication workflow:

- `FrontEnd/src/app/core/services/communication-api.service.ts`
- `FrontEnd/src/app/pages/frontoffice/communication-new/communication-new.ts`
- `FrontEnd/src/app/pages/frontoffice/communication-new/communication-new.html`
- `FrontEnd/src/app/pages/backoffice/communication-inbox/communication-inbox.ts`
- `FrontEnd/src/app/pages/backoffice/communication-inbox/communication-inbox.html`
- `FrontEnd/src/app/pages/backoffice/communication-details/communication-details.ts`
- `FrontEnd/src/app/pages/backoffice/communication-details/communication-details.html`

Deployment/configuration:

- `README.md`
- `docker-compose.full.yml`
- `BackEnd/config-server/src/main/resources/config/communication-service.yml`
- `BackEnd/api-gateway/src/main/resources/application.yml`
- `BackEnd/config-server/src/main/resources/config/api-gateway.yml`

AI/data artifacts:

- `C:\Users\hp\Downloads\data_modeling\NephrosPaidi_Modeling_CORRECTED.ipynb`
- `C:\Users\hp\Downloads\data_modeling\FollowUp_Messages_Cleaned.csv`
- `C:\Users\hp\Downloads\data_modeling\FollowUp_Messages_Prepared.csv`
- `C:\Users\hp\Downloads\DataPrep_Explanation_FollowUp (1) (1).docx`

### Files Modified

- `AI_TRIAGE_DEPLOYMENT_LOG.md`

### Exact Changes Made

- Created this Markdown deployment log.
- Recorded Phase 0 architecture and modeling audit findings.
- No source code, schema, workflow, routing, dashboard, or escalation behavior was changed.

### Modeling Audit Findings

The modeling notebook trains Random Forest, SVM, and XGBoost on `FollowUp_Messages_Prepared.csv`.

Reported notebook results:

- Prepared dataset shape: `661 rows x 81 columns`.
- Class distribution is approximately balanced:
  - `LOW`: 167
  - `MEDIUM`: 174
  - `HIGH`: 161
  - `CRITICAL`: 159
- Best reported model: XGBoost.
- Reported XGBoost metrics:
  - Accuracy: `0.9850`
  - Precision: `0.9854`
  - Recall: `0.9850`
  - Weighted F1: `0.9850`
  - Macro F1: `0.9849`

Important deployment issue: the prepared feature table includes multiple fields that appear unavailable or unsafe at guardian message creation time.

Examples of risky or leaking features:

- `Response_Time_Minutes`
- `Escalated`
- `Assigned_To_Role_DOCTOR`
- `Assigned_To_Role_NURSE`
- `Assigned_To_Role_RECEPTIONIST`
- `Assigned_To_Role_SYSTEM`
- `Status_ARCHIVED`
- `Status_DELIVERED`
- `Status_PENDING`
- `Status_READ`
- `Status_RESOLVED`
- `Status_SENT`
- `Status_TRIAGED`
- `Is_Read_*`
- `AI_Confidence_Score`
- `AI_Intent_Category_*`
- `AI_Sentiment_*`

Why this matters:

- Some fields describe what happened after staff/workflow handling, not what is known when a guardian creates a message.
- Some fields appear to be outputs from another AI or from previous triage decisions.
- If these features are used in production inference, `communication-service` cannot provide them honestly at message creation time.
- If the model learned from future workflow outcome fields, the notebook score is likely inflated and not representative of real deployment performance.

The notebook currently does not appear to export a production model artifact with `joblib`, `pickle`, `save_model`, or an equivalent saved inference pipeline. It trains and evaluates models but does not define a deployable model package.

The prepared CSV also does not include raw `Message_Text`; it contains engineered/encoded fields. The cleaned CSV includes `Message_Text`, but still contains many missing columns and several post-workflow fields.

### Safest Deployable Inference Pipeline

The safest deployable inference pipeline should use only fields available at message creation time, for example:

- `messageText`
- `subject`
- `messageType`
- guardian-selected priority as context, not as the target
- optional text-derived features:
  - character count;
  - word count;
  - medical keyword flag;
  - urgent wording flag;
  - language hint if it can be computed locally;
  - basic punctuation/intensity features.

Fields that should not be required for first inference:

- response time;
- assigned role;
- message status;
- whether the message was escalated;
- read state;
- staff reply data;
- any AI label that is effectively a previous answer to the same triage problem.

Recommended modeling action before real deployment:

1. Build a new exportable inference pipeline from the cleaned/raw message text and creation-time metadata only.
2. Use a scikit-learn `Pipeline` or equivalent saved artifact that includes preprocessing and classifier together.
3. Save the artifact with version metadata.
4. Validate per-class recall, especially `CRITICAL` recall.
5. Add a confidence threshold policy for display, not for automatic routing.

### Deployment Recommendation

Recommended architecture for the first implementation after approval:

`Angular -> API Gateway -> communication-service -> HTTP -> ai-triage-service`

Recommended service boundaries:

- `communication-service` owns messages, queues, roles, audit logs, assignment, and escalation.
- `ai-triage-service` owns model loading and prediction only.
- `ai-triage-service` does not write to the NephroPaidi database.
- Frontend does not call `ai-triage-service` directly.

Recommended communication method:

- Use simple internal HTTP for the first deployment.
- RabbitMQ is not recommended for the initial version because the current need is request-time enrichment, not a complex asynchronous pipeline.
- RabbitMQ can be reconsidered later for async retriage, batching, retry queues, or monitoring pipelines.

Recommended first persisted AI fields on `FollowUpMessage`:

- `aiUrgencyLevel`: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`
- `aiConfidence`
- `aiStatus`: `PENDING`, `SUCCESS`, `FAILED`, `SKIPPED`
- `aiExplanation`
- `aiModelVersion`
- `aiEvaluatedAt`

Recommended first UI value:

- Show AI urgency, confidence, and explanation as support information on staff-facing inbox/details.
- Do not change queue assignment.
- Do not change escalation.
- Do not auto-assign doctors.

### Risks Identified

Architecture risks:

- Accidentally making the AI service a second workflow owner.
- Allowing frontend to depend directly on AI service availability.
- Coupling Java backend deployment to Python ML dependencies if embedded in `communication-service`.

Workflow risks:

- Changing `routeQueue(...)` would alter receptionist/nurse/doctor behavior.
- Replacing `PriorityLevel` with AI urgency would affect current frontend filters and counts.
- Auto-routing `CRITICAL` to doctors would change the business workflow and should require separate approval.

Modeling risks:

- Current model likely uses leakage/post-outcome features.
- No exported inference artifact exists yet.
- The model may not be able to predict from real creation-time inputs without retraining or a new preprocessing pipeline.
- Notebook performance may not represent production performance.

Reliability risks:

- AI service downtime could block message creation if not designed with timeout and fallback.
- Slow model inference could increase guardian message submission latency.

Clinical/product risks:

- Staff may overtrust AI urgency.
- `LOW` predictions could be dangerous if displayed as definitive.
- Explanations must be short and supportive, not diagnostic.

### Rollback Strategy

For Phase 0:

- Delete `AI_TRIAGE_DEPLOYMENT_LOG.md` if the documentation file is not wanted.
- No application behavior needs rollback because no code or configuration behavior was changed.

For future phases:

- Keep AI service separate so it can be stopped without stopping `communication-service`.
- Add AI fields in a backwards-compatible way.
- Use feature flags/config such as `ai.triage.enabled=false`.
- Keep message creation fallback behavior so the application still works if the AI service is unavailable.
- Avoid changing existing routing logic until explicitly approved.

### Why The Phase Was Necessary

This audit prevents deploying a model in a way that looks integrated but is not trustworthy. It also protects the existing communication workflow from accidental changes while identifying the correct integration seam.

### Current Status

- Architecture audit completed.
- Existing communication workflow mapped.
- AI modeling artifacts inspected.
- Critical modeling deployment risk identified: likely leakage and missing production export pipeline.
- No runtime behavior changed.

### Next Recommendation

Before any backend/frontend code changes, decide the modeling path:

1. Approve creating a separate `ai-triage-service` skeleton with `/health` and `/predict`.
2. Approve building/exporting a production-safe model pipeline using only creation-time inputs.
3. Only after a deployable artifact exists, approve adding AI metadata fields to `communication-service`.

Recommended next phase:

- Phase 1A: create an isolated `ai-triage-service` skeleton and model export plan, still with no changes to message routing or dashboards.

## Phase 1A - Isolated AI Service Skeleton And Model Export Plan

### Objective

Create a standalone base service for follow-up message urgency prediction without connecting it to the existing application workflow. Define the clean model export plan needed before production integration.

### Analysis And Reasoning

The existing project is service-oriented, while the model stack is Python-oriented. A separate `ai-triage-service` keeps ML dependencies out of the Java `communication-service` and preserves the current message workflow boundaries.

This phase intentionally does not wire the service into Docker Compose, API Gateway, `communication-service`, Angular, or the database. The service is only a deployable skeleton with stable request/response contracts and a clearly marked fallback predictor.

The fallback predictor exists only so the service can respond while the real model artifact is not ready. It should not be treated as the final ML model.

### Files Inspected

- `BackEnd`
- `BackEnd/microservices`
- `.gitignore`
- `AI_TRIAGE_DEPLOYMENT_LOG.md`

### Files Modified

- `BackEnd/microservices/ai-triage-service/app/__init__.py`
- `BackEnd/microservices/ai-triage-service/app/main.py`
- `BackEnd/microservices/ai-triage-service/app/model_loader.py`
- `BackEnd/microservices/ai-triage-service/app/schemas.py`
- `BackEnd/microservices/ai-triage-service/requirements.txt`
- `BackEnd/microservices/ai-triage-service/Dockerfile`
- `AI_TRIAGE_DEPLOYMENT_LOG.md`

### Exact Changes Made

Created an isolated FastAPI service skeleton:

- `GET /health`
  - returns service status and whether a model artifact was loaded.
- `POST /predict`
  - accepts message creation-time inputs only;
  - returns urgency, confidence, prediction status, explanation, and model version.

Created request/response schemas:

- `TriageRequest`
  - `messageText`
  - `subject`
  - `messageType`
  - `guardianPriority`
- `TriageResponse`
  - `urgencyLevel`
  - `confidence`
  - `status`
  - `explanation`
  - `modelVersion`

Created model loader structure:

- looks for `AI_TRIAGE_MODEL_PATH`, defaulting to `/app/models/followup_triage.joblib`;
- loads the artifact with `joblib` if present;
- exposes `modelLoaded` through `/health`;
- falls back to rule-based placeholder prediction when no artifact is present;
- never accesses NephroPaidi databases;
- never owns message workflow state.

Created service packaging files:

- `requirements.txt`;
- `Dockerfile`.

### Why The Change Was Necessary

This creates a safe deployment boundary for the AI model while keeping the real application untouched. It also gives the future `communication-service` integration a stable internal API contract to call after the real model pipeline is exported.

### Safe Input Features Available At Message Creation Time

Final deployable inference should use only data available when the guardian submits the message:

- `messageText`
- `subject`
- `messageType`
- `guardianPriority`
- derived `character_count`
- derived `word_count`
- derived medical keyword signal
- derived urgent keyword signal
- derived language signal if implemented inside the same preprocessing pipeline
- optional text vector features from `messageText` and `subject`

These features are safe because they do not depend on staff actions, routing outcomes, assignment, response time, read status, or prior AI labels.

### Features That Must Not Be Required By The Production Model

The production inference artifact should not require:

- `Response_Time_Minutes`
- `Escalated`
- `Assigned_To_Role_*`
- `Status_*`
- `Is_Read_*`
- staff response/reply information
- workflow outcome fields
- previous AI-generated labels such as `AI_Intent_Category_*`, `AI_Sentiment_*`, or `AI_Confidence_Score`, unless those are produced by a separate approved model inside the same inference pipeline.

### Clean Model Export Plan

Required retraining/export work:

1. Start from data that includes raw message text, especially `Message_Text`, and target `AI_Urgency_Level`.
2. Build a training dataframe using only creation-time inputs.
3. Split train/test with stratification by urgency.
4. Create a single reproducible preprocessing + model pipeline.
5. Include text preprocessing inside the pipeline, for example:
   - normalization;
   - TF-IDF or equivalent text vectorization;
   - one-hot encoding for `messageType`;
   - one-hot or binary handling for `guardianPriority`;
   - numeric derived text features.
6. Train the selected classifier.
7. Evaluate per-class metrics, with special attention to `CRITICAL` recall.
8. Save one artifact with `joblib`, for example:
   - `followup_triage.joblib`
9. Save metadata beside the artifact, for example:
   - model version;
   - training date;
   - feature contract;
   - urgency label mapping;
   - evaluation metrics.

The artifact should expose a stable inference method through a scikit-learn-compatible pipeline. Inference must use the same preprocessing that was used during training; preprocessing must not be reimplemented differently inside FastAPI.

### Expected Artifact

Recommended artifact:

- `models/followup_triage.joblib`

Recommended metadata:

- `models/followup_triage_metadata.json`

The artifact should accept the same logical fields as `TriageRequest` or a deterministic transformation of those fields.

### Risks Identified

- The fallback predictor is only a placeholder and could be mistaken for the trained model.
- The current `model_loader.py` assumes a future artifact can consume a dictionary of creation-time features. The final export may require adjusting the feature adapter to match the approved pipeline.
- Adding `scikit-learn` and `numpy` to the AI service increases image size, but keeps those dependencies isolated from Java services.
- No real model artifact is bundled yet, so `/predict` currently returns `status = FALLBACK` unless a compatible artifact is mounted.

### Rollback Strategy

- Delete `BackEnd/microservices/ai-triage-service`.
- Revert this section from `AI_TRIAGE_DEPLOYMENT_LOG.md`.
- No existing application behavior needs rollback because this phase does not modify or wire into any existing service.

### Current Status

- Isolated service skeleton created.
- Request/response schema created.
- Placeholder fallback prediction created.
- Dockerfile and requirements created.
- Model export plan documented.
- No `communication-service` changes.
- No frontend changes.
- No workflow changes.

### Next Recommendation

Review and approve one of these next steps:

1. Build the clean training/export script and produce `followup_triage.joblib` from safe creation-time features.
2. Add tests for the isolated AI service contract.
3. After the model artifact is ready, discuss the `communication-service` integration design before modifying Java code.

## Phase 1B - Clean Training Export And Real Artifact

### Objective

Produce a deployable model artifact for the isolated `ai-triage-service` using only safe creation-time inputs and text-derived features. Avoid leaking or future workflow fields even if that reduces reported accuracy.

### Analysis And Reasoning

The original notebook uses `FollowUp_Messages_Prepared.csv`, which contains many fields that are not safe at guardian message creation time. This phase intentionally avoids that prepared feature table for training.

The clean export script uses `FollowUp_Messages_Cleaned.csv` because it contains the raw `Message_Text` and target `AI_Urgency_Level`. It builds an inference pipeline that can be called with the same logical fields expected by `/predict`.

The available cleaned CSV does not contain the application's exact `messageType` field or guardian-submitted priority. To keep the model contract aligned with the application without inventing unavailable training labels, the export script trains with:

- `message_type = OTHER`
- `guardian_priority = UNKNOWN`

This means the first deployable artifact is primarily text-driven. The service can still accept real `messageType` and `guardianPriority` during inference, and the one-hot encoder is configured to ignore unknown categories safely.

### Files Inspected

- `C:\Users\hp\Downloads\data_modeling\FollowUp_Messages_Cleaned.csv`
- `C:\Users\hp\Downloads\data_modeling\FollowUp_Messages_Prepared.csv`
- `BackEnd/microservices/ai-triage-service`
- `AI_TRIAGE_DEPLOYMENT_LOG.md`

### Files Modified

- `BackEnd/microservices/ai-triage-service/app/pipeline.py`
- `BackEnd/microservices/ai-triage-service/app/model_loader.py`
- `BackEnd/microservices/ai-triage-service/training/export_model.py`
- `BackEnd/microservices/ai-triage-service/training/verify_artifact.py`
- `BackEnd/microservices/ai-triage-service/requirements.txt`
- `BackEnd/microservices/ai-triage-service/models/followup_triage.joblib`
- `BackEnd/microservices/ai-triage-service/models/followup_triage_metadata.json`
- `AI_TRIAGE_DEPLOYMENT_LOG.md`

### Exact Changes Made

Created shared preprocessing module:

- `CreationTimeFeatureBuilder` converts request-like dictionaries into a stable feature dataframe.
- Derived features:
  - combined subject + message text;
  - character count;
  - word count;
  - medical keyword flag;
  - urgent keyword flag;
  - critical keyword flag;
  - question mark count;
  - exclamation count.

Created export script:

- `training/export_model.py`
- Reads `FollowUp_Messages_Cleaned.csv`.
- Uses only `Message_Text` and `AI_Urgency_Level` from the dataset.
- Creates safe app-compatible columns:
  - `message_text`
  - `subject`
  - `message_type`
  - `guardian_priority`
- Builds a single sklearn `Pipeline` containing:
  - `CreationTimeFeatureBuilder`;
  - TF-IDF text vectorization;
  - one-hot categorical encoding;
  - numeric feature scaling;
  - balanced Logistic Regression classifier.
- Saves:
  - `models/followup_triage.joblib`
  - `models/followup_triage_metadata.json`

Created verification script:

- `training/verify_artifact.py`
- Loads the exported artifact directly with `joblib`.
- Runs a sample prediction.

Updated `model_loader.py`:

- Uses the loaded artifact's own `classes_` labels when mapping probabilities to urgency.
- This avoids assuming an incorrect class order.

Updated `requirements.txt`:

- Added `pandas`, because the exported preprocessing pipeline uses pandas at runtime.

### Safe Feature Set Used

The exported artifact is based on creation-time and text-derived features only:

- `message_text`
- `subject`
- `message_type`
- `guardian_priority`
- TF-IDF features from text;
- derived character count;
- derived word count;
- derived medical keyword flag;
- derived urgent keyword flag;
- derived critical keyword flag;
- punctuation intensity counts.

### Explicitly Excluded Leakage/Future Fields

The export script does not use:

- assigned role;
- queue;
- status;
- escalation outcome;
- response time;
- read state;
- staff actions;
- previous AI intent labels;
- previous AI sentiment labels;
- previous AI confidence values;
- prepared one-hot workflow columns.

### Artifact Produced

- `BackEnd/microservices/ai-triage-service/models/followup_triage.joblib`
- `BackEnd/microservices/ai-triage-service/models/followup_triage_metadata.json`

Metadata includes:

- model version;
- training time;
- source dataset;
- training/test row counts;
- safe feature contract;
- excluded leakage families;
- labels;
- metrics;
- caution notes.

### Validation Result

Export command:

```powershell
python BackEnd\microservices\ai-triage-service\training\export_model.py --input "C:\Users\hp\Downloads\data_modeling\FollowUp_Messages_Cleaned.csv" --output-dir BackEnd\microservices\ai-triage-service\models --version followup-triage-safe-v1
```

Export result:

- Model written to `BackEnd\microservices\ai-triage-service\models\followup_triage.joblib`
- Metadata written to `BackEnd\microservices\ai-triage-service\models\followup_triage_metadata.json`
- Accuracy: `1.0000`
- Weighted F1: `1.0000`
- Macro F1: `1.0000`

Verification command:

```powershell
python BackEnd\microservices\ai-triage-service\training\verify_artifact.py --model BackEnd\microservices\ai-triage-service\models\followup_triage.joblib
```

Verification result:

- Prediction: `CRITICAL`
- Confidence: `0.8384`

Syntax validation:

- All Python files under `BackEnd/microservices/ai-triage-service` parsed successfully with `ast.parse`.

Loader validation note:

- Direct artifact verification succeeded.
- Full `TriageModel`/FastAPI loader execution was not run in the current local Python environment because `pydantic` is not installed globally.
- `pydantic` is declared in `BackEnd/microservices/ai-triage-service/requirements.txt`, so it will be available in the service runtime image after dependency installation.

### Accuracy Caveat

The clean artifact still reports perfect test metrics. This should be treated cautiously.

Possible explanations:

- the dataset may be synthetic;
- messages may be highly templated;
- urgency labels may be strongly encoded in the wording;
- train/test split may not fully represent real-world variation.

This is still more deployable than the previous prepared-feature model because it avoids workflow leakage, but it should be validated later on fresh real guardian messages before using it for automatic routing.

### Why The Change Was Necessary

The application needs a real deployable artifact, not only a notebook score. This phase produces an artifact that includes preprocessing and model together, so inference can use the same transformations as training.

### Risks Identified

- The artifact is trained on only 661 rows.
- The dataset does not contain real application `messageType`, so training cannot fully learn that field yet.
- The dataset does not contain real guardian priority, so training cannot fully learn that field yet.
- Perfect metrics may create false confidence.
- The model should not be used for automatic assignment or routing without future validation.

### Rollback Strategy

- Delete `BackEnd/microservices/ai-triage-service/models/followup_triage.joblib`.
- Delete `BackEnd/microservices/ai-triage-service/models/followup_triage_metadata.json`.
- Remove `training/export_model.py`, `training/verify_artifact.py`, and `app/pipeline.py` if the export approach is rejected.
- Revert `model_loader.py` to fallback-only behavior.
- No existing application behavior needs rollback because no existing service is wired to this artifact.

### Current Status

- Clean export script created.
- Runtime-compatible preprocessing pipeline created.
- Real model artifact produced.
- Metadata produced.
- Direct artifact verification passed.
- No `communication-service` changes.
- No frontend changes.
- No Docker Compose changes.
- No routing, queue, dashboard, escalation, or workflow behavior changes.

### Next Recommendation

Before integrating with `communication-service`, approve one of these isolated follow-up steps:

1. Add isolated FastAPI tests for `/health` and `/predict`.
2. Build the Docker image locally and run `/health` and `/predict`.
3. Review the Java integration design only, without implementing it yet.

## Phase 1C - Isolated AI Service Validation And Stabilization

### Objective

Stabilize and validate the isolated `ai-triage-service` before any integration with the existing NephroPaidi application workflow.

### Analysis And Reasoning

The service should be testable by itself before `communication-service` ever calls it. This phase adds endpoint contract tests, local run documentation, Docker run documentation, and generated-file ignores. It also adjusts the Docker image so the exported model artifact is copied into the runtime image.

No existing NephroPaidi service is connected to the AI service in this phase.

### Files Inspected

- `BackEnd/microservices/ai-triage-service/Dockerfile`
- `BackEnd/microservices/ai-triage-service/requirements.txt`
- `BackEnd/microservices/ai-triage-service/app/main.py`
- `BackEnd/microservices/ai-triage-service/app/model_loader.py`
- `BackEnd/microservices/ai-triage-service/models/followup_triage.joblib`
- local Python runtime dependency availability
- Docker daemon availability

### Files Modified

- `BackEnd/microservices/ai-triage-service/Dockerfile`
- `BackEnd/microservices/ai-triage-service/.gitignore`
- `BackEnd/microservices/ai-triage-service/README.md`
- `BackEnd/microservices/ai-triage-service/requirements-dev.txt`
- `BackEnd/microservices/ai-triage-service/tests/test_api.py`
- `AI_TRIAGE_DEPLOYMENT_LOG.md`

### Exact Changes Made

Dockerfile:

- Added `COPY models ./models` so the runtime image includes `models/followup_triage.joblib`.
- This makes `/health` able to report `modelLoaded: true` when the artifact is present.

Service docs:

- Added `BackEnd/microservices/ai-triage-service/README.md`.
- Documented local Python 3.12 run commands.
- Documented Docker build/run commands.
- Documented `/health` and `/predict` examples.
- Documented artifact-only verification command.

Tests:

- Added `BackEnd/microservices/ai-triage-service/tests/test_api.py`.
- Test coverage includes:
  - `GET /health` with model loaded;
  - `POST /predict` response contract for realistic LOW, MEDIUM, HIGH, and CRITICAL-style guardian messages;
  - fallback response behavior when the model artifact path is missing.

Development dependencies:

- Added `BackEnd/microservices/ai-triage-service/requirements-dev.txt` with `pytest` and `httpx`.

Local ignore:

- Added `.gitignore` for `.deps`, `.venv`, `__pycache__`, and `*.pyc`.
- Removed generated `.deps` and `.venv` directories after failed local dependency attempts.

### Commands To Run The Service Locally

Recommended runtime: Python 3.12.

```powershell
cd BackEnd/microservices/ai-triage-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
uvicorn app.main:app --host 127.0.0.1 --port 8000
```

Health check:

```powershell
Invoke-RestMethod http://127.0.0.1:8000/health
```

Expected format:

```json
{
  "status": "UP",
  "modelLoaded": true
}
```

Prediction example:

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri http://127.0.0.1:8000/predict `
  -ContentType "application/json" `
  -Body '{"messageText":"Fievre a 39 et vomissements depuis hier soir.","subject":"Symptomes","messageType":"MEDICAL","guardianPriority":"HIGH"}'
```

Expected response format:

```json
{
  "urgencyLevel": "LOW | MEDIUM | HIGH | CRITICAL",
  "confidence": 0.0,
  "status": "SUCCESS | FALLBACK | FAILED",
  "explanation": "...",
  "modelVersion": "..."
}
```

### Commands To Run With Docker

```powershell
docker build -t nephropaidi-ai-triage BackEnd/microservices/ai-triage-service
docker run --rm -p 8000:8000 nephropaidi-ai-triage
```

### Commands To Run Tests

Recommended runtime: Python 3.12.

```powershell
cd BackEnd/microservices/ai-triage-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt -r requirements-dev.txt
pytest
```

### Validation Results

Artifact-only verification command:

```powershell
python BackEnd\microservices\ai-triage-service\training\verify_artifact.py --model BackEnd\microservices\ai-triage-service\models\followup_triage.joblib
```

Result:

- Prediction: `CRITICAL`
- Confidence: `0.8384`

Syntax validation:

- All Python files under `BackEnd/microservices/ai-triage-service` parsed successfully with `ast.parse`.

Endpoint test status:

- FastAPI endpoint tests were added but could not be executed on the current host.
- Current host Python is `3.14`.
- `fastapi`, `pydantic`, `uvicorn`, `pytest`, and `httpx` were not installed.
- Installing the full AI service requirements into an isolated `.deps` folder failed because `scikit-learn==1.5.2` has no compatible wheel for this Python 3.14 environment and the machine lacks local C/C++ build tooling.
- Installing only FastAPI/test dependencies also failed because `pydantic-core` attempted a local native build for Python 3.14 and could not find the MSVC linker.
- Docker validation could not run because Docker Desktop was not reachable:
  - `failed to connect to the docker API`
  - `dockerDesktopLinuxEngine` pipe was missing.

Conclusion:

- The artifact itself loads and predicts through the direct verification script.
- Endpoint contract tests are ready and should pass in the intended Python 3.12 container/runtime.
- Full `/health` and `/predict` HTTP validation is blocked by local runtime/tooling, not by application code.

### Fallback Behavior Verification

Implemented in `tests/test_api.py`:

- The test points `AI_TRIAGE_MODEL_PATH` to a missing artifact.
- It calls `POST /predict`.
- Expected response:
  - `status = FALLBACK`
  - `modelVersion = fallback-rules-v0`
  - urgency remains one of the supported urgency enum values.

This test is ready but not executed on this host for the dependency reasons listed above.

### Why The Change Was Necessary

The AI service now has a documented and testable contract before any Java integration is attempted. Future `communication-service` code can rely on a stable response shape:

- `urgencyLevel`
- `confidence`
- `status`
- `explanation`
- `modelVersion`

### Risks Identified

- Host validation depends on Python 3.12 or a functioning Docker runtime.
- Current host Python 3.14 is too new for the pinned native dependencies used here.
- Endpoint tests are present but not yet green in this machine environment.
- Dockerfile now copies the model artifact into the image; if the artifact becomes large later, image size should be reviewed.

### Rollback Strategy

- Remove `BackEnd/microservices/ai-triage-service/tests/test_api.py`.
- Remove `BackEnd/microservices/ai-triage-service/requirements-dev.txt`.
- Remove `BackEnd/microservices/ai-triage-service/README.md`.
- Revert the Dockerfile `COPY models ./models` line if model bundling is rejected.
- Remove `BackEnd/microservices/ai-triage-service/.gitignore` if not wanted.
- No existing NephroPaidi application behavior requires rollback because no existing service was modified or wired to AI.

### Current Status

- Service run instructions documented.
- Docker run instructions documented.
- Endpoint tests added.
- Artifact verification passed.
- Python syntax validation passed.
- Docker/HTTP endpoint validation blocked by local environment.
- No `communication-service` changes.
- No frontend changes.
- No Docker Compose changes.
- No routing, queue, dashboard, escalation, or workflow behavior changes.

### Next Recommendation

Start Docker Desktop or use Python 3.12, then run:

```powershell
cd BackEnd/microservices/ai-triage-service
python -m pip install -r requirements.txt -r requirements-dev.txt
pytest
```

After endpoint tests pass, the next architecture step should be a design-only review of how `communication-service` will call this service with timeout/fallback and store AI metadata without changing routing.

## Phase 1D - Docker Validation Of Isolated AI Service

### Objective

Validate the isolated `ai-triage-service` through Docker now that Docker Desktop is available. Confirm model loading, `/health`, `/predict`, fallback behavior, and isolated tests before any integration with existing application services.

### Analysis And Reasoning

Docker validation is important because the host Python runtime is `3.14`, while the service Dockerfile intentionally uses Python `3.12-slim`. The earlier host validation was blocked by native dependency builds. Docker gives the service its intended runtime and therefore is the correct validation environment.

This phase remained isolated. No existing NephroPaidi service was modified or connected to the AI service.

### Files Inspected

- `BackEnd/microservices/ai-triage-service/Dockerfile`
- `BackEnd/microservices/ai-triage-service/requirements.txt`
- `BackEnd/microservices/ai-triage-service/models/followup_triage.joblib`
- `BackEnd/microservices/ai-triage-service/models/followup_triage_metadata.json`
- `BackEnd/microservices/ai-triage-service/tests/test_api.py`

### Files Modified

- `BackEnd/microservices/ai-triage-service/requirements.txt`
- `BackEnd/microservices/ai-triage-service/training/export_model.py`
- `BackEnd/microservices/ai-triage-service/models/followup_triage.joblib`
- `BackEnd/microservices/ai-triage-service/models/followup_triage_metadata.json`
- `AI_TRIAGE_DEPLOYMENT_LOG.md`

### Exact Changes Made

During Docker endpoint validation, the service reported `modelLoaded: true`, but `/predict` initially fell back with:

- `status = FAILED`
- `explanation = Model inference failed; fallback used. Reason: AttributeError`

Root cause:

- The model artifact was exported locally with scikit-learn `1.8.0`.
- The Docker runtime installed scikit-learn `1.5.2`.
- The artifact loaded, but inference failed due to estimator version mismatch.

Fix:

- Updated `BackEnd/microservices/ai-triage-service/requirements.txt`:
  - `scikit-learn==1.5.2` -> `scikit-learn==1.8.0`
- Updated `training/export_model.py` metadata output to record `sklearnVersion`.
- Re-exported `models/followup_triage.joblib`.
- Re-exported `models/followup_triage_metadata.json`.
- Rebuilt the Docker image.

### Docker Build Command

```powershell
docker build -t nephropaidi-ai-triage-validation BackEnd\microservices\ai-triage-service
```

Final result:

- Build succeeded.
- Runtime installed:
  - FastAPI `0.115.6`
  - Pydantic `2.10.4`
  - scikit-learn `1.8.0`
  - pandas `2.2.3`
  - numpy `2.1.3`
- Image name: `nephropaidi-ai-triage-validation`

### Container Run Command

```powershell
docker run -d --rm --name nephropaidi-ai-triage-validation -p 8000:8000 nephropaidi-ai-triage-validation
```

Final result:

- Container started successfully.
- Uvicorn started on `0.0.0.0:8000`.

### Health Endpoint Test

Command:

```powershell
Invoke-RestMethod http://127.0.0.1:8000/health
```

Result:

```json
{
  "status": "UP",
  "modelLoaded": true
}
```

### Prediction Endpoint Tests

Command pattern:

```powershell
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8000/predict -ContentType "application/json" -Body "<payload>"
```

Test results:

| Intended Example | Returned Urgency | Confidence | Status | Model Version |
|---|---:|---:|---|---|
| LOW administrative document pickup | LOW | 0.3949 | SUCCESS | followup_triage |
| MEDIUM medication timing question | MEDIUM | 0.6515 | SUCCESS | followup_triage |
| HIGH fever/vomiting concern | CRITICAL | 0.9108 | SUCCESS | followup_triage |
| CRITICAL breathing difficulty/severe pain | CRITICAL | 0.8986 | SUCCESS | followup_triage |

All responses used the stable contract:

- `urgencyLevel`
- `confidence`
- `status`
- `explanation`
- `modelVersion`

Observation:

- The HIGH-style fever/vomiting example was classified as `CRITICAL`.
- This is not a service failure; it is a model behavior that should be reviewed clinically before any future automatic routing.
- For the planned advisory/support layer, this is acceptable as long as it is displayed as AI support, not used as automatic assignment.

### Fallback Behavior Test

Fallback container command:

```powershell
docker run -d --rm --name nephropaidi-ai-triage-fallback-validation --publish 127.0.0.1:8010:8000 --env AI_TRIAGE_MODEL_PATH=/app/models/missing.joblib nephropaidi-ai-triage-validation
```

Health result:

```json
{
  "status": "UP",
  "modelLoaded": false
}
```

Prediction result:

```json
{
  "urgencyLevel": "HIGH",
  "confidence": 0.55,
  "status": "FALLBACK",
  "explanation": "Fallback detected urgent wording or guardian high priority.",
  "modelVersion": "fallback-rules-v0"
}
```

Conclusion:

- Missing artifact does not crash the service.
- Fallback response format is stable.
- Future `communication-service` integration can distinguish `SUCCESS`, `FALLBACK`, and `FAILED`.

### Isolated Test Suite

Command:

```powershell
docker run --rm -v "${PWD}:/work" -w /work/BackEnd/microservices/ai-triage-service python:3.12-slim sh -c "pip install --no-cache-dir -r requirements.txt -r requirements-dev.txt && pytest -q"
```

Result:

```text
3 passed in 3.98s
```

Covered:

- `/health` reports loaded model.
- `/predict` response contract works for realistic examples.
- missing artifact path returns fallback behavior.

### Why The Change Was Necessary

The Docker validation found and fixed a real runtime compatibility problem before integration. This avoids a future situation where `communication-service` would call an AI service that appears healthy but silently falls back on every prediction.

### Risks Identified

- Runtime dependency versions must stay aligned with the exported artifact.
- The model may classify fever/vomiting as `CRITICAL`; this should be reviewed and documented as model behavior before any workflow automation.
- `modelVersion` currently defaults to the artifact stem (`followup_triage`) unless `AI_TRIAGE_MODEL_VERSION` is provided at runtime.
- The Docker image includes the model artifact directly; future larger artifacts may increase image size.

### Rollback Strategy

- Revert `requirements.txt` to the prior scikit-learn version only if the artifact is also re-exported with that version.
- Re-run `training/export_model.py` to regenerate the artifact and metadata.
- Stop/remove validation containers:
  - `docker stop nephropaidi-ai-triage-validation`
  - `docker stop nephropaidi-ai-triage-fallback-validation`
- No existing application behavior requires rollback because no existing service was modified or integrated.

### Current Status

- Docker image build passed.
- Container startup passed.
- `/health` passed with `modelLoaded: true`.
- `/predict` passed for LOW, MEDIUM, HIGH-style, and CRITICAL-style examples.
- Missing-artifact fallback passed.
- Isolated pytest suite passed: `3 passed`.
- No `communication-service` changes.
- No frontend changes.
- No Docker Compose changes.
- No routing, queue, dashboard, escalation, or workflow behavior changes.

### Next Recommendation

Proceed to a design-only review for `communication-service` integration:

- where to store AI metadata;
- timeout/fallback behavior;
- exact DTO fields;
- migration plan;
- no routing changes;
- no dashboard behavior changes without approval.

## Phase 2A - Communication-Service AI Triage Metadata Persistence

### Objective

Prepare `communication-service` to store future AI triage results on follow-up messages without calling the AI service and without changing existing workflow behavior.

### Analysis And Reasoning

The current communication workflow already owns guardian follow-up messages, staff queues, message statuses, assignment, replies, and nurse-to-doctor escalation. AI triage metadata belongs on `FollowUpMessage` because it describes a message, but it must remain separate from existing `PriorityLevel`, `MessageQueue`, and `MessageStatus`.

This phase adds only nullable persistence and response support. No creation logic writes these fields yet, so existing message creation continues to produce null AI metadata.

The project already uses Flyway migrations in `communication-service/src/main/resources/db/migration`, so a new migration was added instead of relying only on Hibernate `ddl-auto: update`.

### Files Inspected

- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/domain/entity/FollowUpMessage.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/dto/response/FollowUpMessageResponse.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/dto/response/CreateMessageResponse.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/mapper/MessageMapper.java`
- `BackEnd/microservices/communication-service/src/main/resources/db/migration`
- `BackEnd/config-server/src/main/resources/config/communication-service.yml`

### Files Modified

- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/domain/entity/FollowUpMessage.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/domain/enums/AiUrgencyLevel.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/domain/enums/AiTriageStatus.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/dto/response/FollowUpMessageResponse.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/dto/response/CreateMessageResponse.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/mapper/MessageMapper.java`
- `BackEnd/microservices/communication-service/src/main/resources/db/migration/V6__add_ai_triage_metadata_to_follow_up_messages.sql`
- `AI_TRIAGE_DEPLOYMENT_LOG.md`

### Exact Changes Made

Added enum `AiUrgencyLevel`:

- `LOW`
- `MEDIUM`
- `HIGH`
- `CRITICAL`

Added enum `AiTriageStatus`:

- `PENDING`
- `SUCCESS`
- `FALLBACK`
- `FAILED`
- `SKIPPED`

Added nullable fields to `FollowUpMessage`:

- `aiUrgencyLevel`
- `aiConfidence`
- `aiTriageStatus`
- `aiExplanation`
- `aiModelVersion`
- `aiEvaluatedAt`

Added the same fields to response DTOs:

- `FollowUpMessageResponse`
- `CreateMessageResponse`

Updated `MessageMapper` to map the new fields from entity to response.

Added Flyway migration:

- `V6__add_ai_triage_metadata_to_follow_up_messages.sql`

Migration adds nullable columns:

- `ai_urgency_level varchar(32)`
- `ai_confidence double precision`
- `ai_triage_status varchar(32)`
- `ai_explanation varchar(500)`
- `ai_model_version varchar(100)`
- `ai_evaluated_at timestamptz`

Migration also adds indexes:

- `idx_follow_up_messages_ai_urgency_level`
- `idx_follow_up_messages_ai_triage_status`

### Behavior Preserved

No changes were made to:

- `FollowUpMessageServiceImpl.create(...)`
- `routeQueue(...)`
- `staffInbox(...)`
- `take(...)`
- `unassign(...)`
- `markRead(...)`
- `reply(...)`
- `escalate(...)`
- `close(...)`
- frontend code;
- Docker Compose;
- API Gateway;
- dashboards;
- queue logic;
- nurse-to-doctor escalation.

Existing guardian message creation still works with all AI fields null.

### Why The Change Was Necessary

Future AI integration needs a place to persist AI triage metadata. Adding nullable fields now creates the database and response contract without introducing any AI dependency or workflow behavior change.

### Validation

Compile command:

```powershell
cd BackEnd/microservices/communication-service
mvn -q -DskipTests compile
```

Result:

- Passed.

Focused message-flow test command:

```powershell
cd BackEnd/microservices/communication-service
mvn -q -Dtest=FollowUpMessageServiceImplTest test
```

Result:

- Passed.

This validates that the core message creation, reply, take, close, and nurse escalation tests still pass with the new nullable AI metadata fields.

### Risks Identified

- API responses now include additional nullable fields. This is generally backwards-compatible for JSON clients, but generated clients would need refresh later.
- The migration adds indexes that are useful for future filtering but slightly increase write/index storage overhead.
- `CreateMessageResponse` now includes nullable AI fields even though no AI call happens yet. This keeps the response contract future-ready but may be visually unused until frontend work is approved.
- The project still has `ddl-auto: update`; Flyway is the explicit schema strategy used here to keep the change trackable.

### Rollback Strategy

Code rollback:

- Remove `AiUrgencyLevel.java`.
- Remove `AiTriageStatus.java`.
- Remove AI fields from `FollowUpMessage`.
- Remove AI fields from `FollowUpMessageResponse`.
- Remove AI fields from `CreateMessageResponse`.
- Remove AI mappings from `MessageMapper`.

Database rollback if already migrated:

```sql
drop index if exists idx_follow_up_messages_ai_urgency_level;
drop index if exists idx_follow_up_messages_ai_triage_status;

alter table follow_up_messages
    drop column if exists ai_urgency_level,
    drop column if exists ai_confidence,
    drop column if exists ai_triage_status,
    drop column if exists ai_explanation,
    drop column if exists ai_model_version,
    drop column if exists ai_evaluated_at;
```

No workflow rollback is needed because workflow behavior was not changed.

### Current Status

- Nullable AI persistence fields added.
- DTO/mapper support added.
- Flyway migration added.
- Compile passed.
- Focused message-flow tests passed.
- No AI service calls added.
- No frontend changes.
- No Docker Compose changes.
- No routing, queue, dashboard, escalation, or workflow behavior changes.

### Next Recommendation

Before implementation, do a design-only review for the next phase:

- internal HTTP client shape;
- timeout value;
- fallback policy;
- config property names;
- when to call AI during message creation;
- how to write audit logs;
- confirmation that routing still remains unchanged.

## Phase 2B - Communication-Service Advisory AI Triage Integration

### Objective

Integrate `communication-service` with `ai-triage-service` as advisory metadata only. When a guardian creates a follow-up message, `communication-service` calls `/predict`, stores AI metadata, and continues the existing workflow unchanged.

### Analysis And Reasoning

The AI service has already been validated in isolation. The correct integration point is inside `FollowUpMessageServiceImpl.create(...)`, because that is where guardian follow-up messages are created and persisted.

The AI result is stored on the message but is not used to determine queue, assignment, status, dashboards, or escalation. Existing `routeQueue(messageType, priority)` remains the only routing decision.

The call is failure-isolated:

- if AI succeeds, metadata is stored;
- if AI returns fallback, fallback metadata is stored;
- if AI fails, times out, or even throws unexpectedly, message creation continues and `aiTriageStatus = FAILED`.

### Files Inspected

- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/CommunicationServiceApplication.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/client/FeignClientConfiguration.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/service/impl/FollowUpMessageServiceImpl.java`
- `BackEnd/microservices/communication-service/src/test/java/tn/esprit/spring/communicationservice/service/impl/FollowUpMessageServiceImplTest.java`
- `BackEnd/config-server/src/main/resources/config/communication-service.yml`

### Files Modified

- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/ai/AiTriageProperties.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/ai/AiTriageRequest.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/ai/AiTriageResponse.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/ai/AiTriageResult.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/ai/AiTriageClient.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/service/impl/FollowUpMessageServiceImpl.java`
- `BackEnd/microservices/communication-service/src/test/java/tn/esprit/spring/communicationservice/service/impl/FollowUpMessageServiceImplTest.java`
- `BackEnd/config-server/src/main/resources/config/communication-service.yml`
- `AI_TRIAGE_DEPLOYMENT_LOG.md`

### Exact Changes Made

Added AI triage config properties:

- `ai.triage.enabled`
- `ai.triage.base-url`
- `ai.triage.timeout-millis`

Defaults:

- `enabled = true`
- `baseUrl = http://localhost:8000`
- `timeoutMillis = 1500`

Added config-server entries:

```yaml
ai:
  triage:
    enabled: ${AI_TRIAGE_ENABLED:true}
    base-url: ${AI_TRIAGE_SERVICE_BASE_URL:http://localhost:8000}
    timeout-millis: ${AI_TRIAGE_TIMEOUT_MILLIS:1500}
```

Added Java AI request/response/client classes:

- `AiTriageRequest`
- `AiTriageResponse`
- `AiTriageResult`
- `AiTriageProperties`
- `AiTriageClient`

`AiTriageClient`:

- calls `POST {baseUrl}/predict`;
- uses connect/read timeout from config;
- converts empty responses, HTTP/client errors, timeouts, and invalid config into `FAILED` or `SKIPPED` result objects;
- does not throw errors to guardian message creation for normal AI call failures.

Updated `FollowUpMessageServiceImpl.create(...)`:

- keeps existing patient resolution, message initialization, status, priority, and queue routing unchanged;
- calls AI after setting the existing queue/status/timestamps;
- stores:
  - `aiUrgencyLevel`
  - `aiConfidence`
  - `aiTriageStatus`
  - `aiExplanation`
  - `aiModelVersion`
  - `aiEvaluatedAt`
- saves the message normally;
- still writes only existing `CREATED` and `ROUTED` audits.

Added a defensive catch in `FollowUpMessageServiceImpl`:

- if `AiTriageClient` unexpectedly throws a runtime exception, creation continues with:
  - `aiTriageStatus = FAILED`
  - short failure explanation
  - `aiEvaluatedAt = now`

Updated tests:

- existing create test now verifies `SKIPPED` metadata can be returned without changing message creation;
- added success test verifying AI metadata is stored and existing routing remains unchanged;
- added failure test verifying creation continues and queue routing remains unchanged when the AI client throws.

### Behavior Preserved

No changes were made to:

- routing logic;
- `routeQueue(...)`;
- queues;
- staff inbox authorization;
- receptionist/nurse/doctor behavior;
- nurse-to-doctor escalation;
- frontend;
- dashboard behavior;
- Docker Compose.

AI urgency is not used for assignment, queue selection, escalation, or dashboard filtering.

### Validation

Compile:

```powershell
cd BackEnd/microservices/communication-service
mvn -q -DskipTests compile
```

Result:

- Passed.

Focused tests:

```powershell
cd BackEnd/microservices/communication-service
mvn -q -Dtest=FollowUpMessageServiceImplTest test
```

Result:

- Passed.

Full communication-service tests:

```powershell
cd BackEnd/microservices/communication-service
mvn -q test
```

Result:

- Passed.

Important validation note:

- The full Spring test context connected to the configured communication database through the running config server.
- Flyway applied migration V6 to that configured database during the test run:
  - `Migrating schema "public" to version "6 - add ai triage metadata to follow up messages"`
  - `Successfully applied 1 migration`

### Risks Identified

- In Docker, `AI_TRIAGE_SERVICE_BASE_URL` still needs future Compose wiring. If left as `http://localhost:8000` inside the communication-service container, the AI call will fail and message creation will continue with `aiTriageStatus = FAILED`.
- Message creation now waits up to `ai.triage.timeout-millis` for AI. The default is intentionally short: `1500ms`.
- The AI client currently creates a small `RestClient` per call. This is acceptable for first integration but could be optimized later.
- API responses now may include real AI metadata immediately after creation once the AI service URL is reachable.
- No audit entry is written for AI triage yet. That can be added in a later approved phase if desired.

### Rollback Strategy

Code rollback:

- Remove the `communicationservice.ai` package.
- Remove `AiTriageClient` dependency from `FollowUpMessageServiceImpl`.
- Remove `applyAiTriage(...)`.
- Remove AI-specific test changes.
- Remove `ai.triage` properties from `communication-service.yml`.

Database rollback:

- Not required to rollback this phase specifically; Phase 2A already added nullable columns.
- If fully rolling back AI metadata support, use the Phase 2A column/index rollback SQL.

Operational rollback:

- Set `AI_TRIAGE_ENABLED=false`.
- Or set `AI_TRIAGE_TIMEOUT_MILLIS` very low.
- Message creation will continue without changing routing.

### Current Status

- AI client added.
- AI request/response DTOs added.
- Config properties added.
- Guardian message creation calls AI in a failure-isolated way.
- AI metadata is stored when available.
- Existing routing remains unchanged.
- Existing workflow remains unchanged.
- Compile passed.
- Focused tests passed.
- Full `communication-service` tests passed.
- No frontend changes.
- No Docker Compose changes.
- No dashboard changes.
- No escalation changes.

### Next Recommendation

Next phase should be Docker Compose/runtime wiring only:

- add `ai-triage-service` to Compose;
- set `AI_TRIAGE_SERVICE_BASE_URL=http://ai-triage-service:8000` for `communication-service`;
- keep frontend, routing, queues, and escalation unchanged;
- validate creating a guardian message stores AI metadata while retaining the existing queue.

## Phase 2C - Docker/Config Wiring For AI Triage Service

### Objective

Run the validated `ai-triage-service` as part of the full project stack and configure `communication-service` to reach it by container name.

### Analysis And Reasoning

The AI service is now a separate container on the same Docker network as the existing backend services. `communication-service` receives the AI service URL through environment variables, so the service remains configurable and can be disabled without code changes.

The AI service is not a workflow owner. It only returns prediction metadata. `communication-service` still owns persistence, routing, queues, assignment, and escalation.

The AI service was not added as a hard runtime dependency for message creation. Even if it is down, the existing Java integration catches failures and continues message creation with failed AI metadata.

### Files Inspected

- `docker-compose.full.yml`
- `docker-compose.dev.yml`
- `scripts/start-stack.ps1`
- `scripts/start-stack.sh`

### Files Modified

- `docker-compose.full.yml`
- `scripts/start-stack.ps1`
- `scripts/start-stack.sh`
- `AI_TRIAGE_DEPLOYMENT_LOG.md`

### Exact Changes Made

Added service `ai-triage-service` to `docker-compose.full.yml`:

- build context: `./BackEnd/microservices/ai-triage-service`
- container name: `nephro-ai-triage-service`
- network: `nephro-net`
- port mapping: `8000:8000`
- healthcheck: `GET /health` through Python `urllib.request`
- configurable model env:
  - `AI_TRIAGE_MODEL_PATH`
  - `AI_TRIAGE_MODEL_VERSION`
- resource limits:
  - `cpus: "0.50"`
  - `mem_limit: "512m"`

Configured `communication-service` environment:

- `AI_TRIAGE_ENABLED=${AI_TRIAGE_ENABLED:-true}`
- `AI_TRIAGE_SERVICE_BASE_URL=${AI_TRIAGE_SERVICE_BASE_URL:-http://ai-triage-service:8000}`
- `AI_TRIAGE_TIMEOUT_MILLIS=${AI_TRIAGE_TIMEOUT_MILLIS:-1500}`

Updated stack startup scripts:

- `scripts/start-stack.ps1`
- `scripts/start-stack.sh`

Both now start `ai-triage-service` with backend services and wait for:

```text
http://localhost:8000/health
```

### Behavior Preserved

No changes were made to:

- routing logic;
- queues;
- receptionist/nurse/doctor behavior;
- nurse-to-doctor escalation;
- frontend;
- dashboard behavior;
- AI model behavior.

AI remains advisory metadata only.

### Docker Commands To Build/Run/Test

Build/start only AI and communication services through Compose:

```powershell
docker compose -p nephropaidi -f docker-compose.full.yml up -d --build ai-triage-service communication-service
```

Start the full priority-aware stack:

```powershell
.\scripts\start-stack.ps1
```

Linux/macOS:

```bash
bash ./scripts/start-stack.sh
```

Check rendered Compose config:

```powershell
docker compose -p nephropaidi -f docker-compose.full.yml config --quiet
docker compose -p nephropaidi -f docker-compose.full.yml config
```

Check AI health:

```powershell
Invoke-RestMethod http://127.0.0.1:8000/health
```

Expected:

```json
{
  "status": "UP",
  "modelLoaded": true
}
```

Check containers:

```powershell
docker compose -p nephropaidi -f docker-compose.full.yml ps
docker logs nephro-ai-triage-service --tail 100
docker logs nephro-communication-service --tail 100
```

### Validation Result

Static Compose validation:

```powershell
docker compose -p nephropaidi -f docker-compose.full.yml config --quiet
```

Result:

- Passed.

Rendered Compose config confirmed:

- `ai-triage-service` exists.
- `communication-service` has:
  - `AI_TRIAGE_ENABLED: "true"`
  - `AI_TRIAGE_SERVICE_BASE_URL: http://ai-triage-service:8000`
  - `AI_TRIAGE_TIMEOUT_MILLIS: "1500"`

Runtime validation command attempted:

```powershell
docker compose -p nephropaidi -f docker-compose.full.yml up -d --build ai-triage-service communication-service
```

Result:

- Command exceeded the 10 minute timeout.
- After the timeout, Docker Desktop began returning API errors/timeouts:
  - `500 Internal Server Error`
  - Docker API route: `/containers/json`
  - Docker API route: `/version`
- Subsequent `docker ps`, `docker logs`, and endpoint health checks timed out or failed because the Docker engine was not responding normally.

Runtime tests not completed in this phase because Docker Desktop became unhealthy:

- AI service `/health`
- communication-service create-message flow
- AI metadata persistence through real HTTP call
- message creation fallback while AI service unavailable

### Risks Identified

- Docker Desktop can become unstable during long multi-service rebuilds on this machine.
- `scripts/start-stack.*` now wait for AI health; if the AI image fails to build/start, the helper script will stop. Direct Compose usage still allows starting services selectively.
- Because `communication-service` handles AI failure gracefully, runtime message creation remains resilient if `ai-triage-service` is unavailable after startup.
- `AI_TRIAGE_SERVICE_BASE_URL` must remain `http://ai-triage-service:8000` inside Docker. `localhost:8000` from inside `communication-service` would point to the communication-service container itself.

### Rollback Strategy

Docker/config rollback:

- Remove `ai-triage-service` from `docker-compose.full.yml`.
- Remove `AI_TRIAGE_*` environment variables from `communication-service` in `docker-compose.full.yml`.
- Remove `ai-triage-service` from `scripts/start-stack.ps1`.
- Remove `ai-triage-service` from `scripts/start-stack.sh`.

Operational rollback without code changes:

```powershell
$env:AI_TRIAGE_ENABLED="false"
docker compose -p nephropaidi -f docker-compose.full.yml up -d --build communication-service
```

No routing, queue, dashboard, or escalation rollback is needed because none was changed.

### Current Status

- Compose file updated.
- Startup scripts updated.
- Compose static config validation passed.
- Docker runtime validation was attempted but blocked by Docker Desktop API errors after a long Compose build/start timeout.
- No frontend changes.
- No routing changes.
- No queue changes.
- No dashboard changes.
- No escalation changes.

### Next Recommendation

Restart Docker Desktop, then rerun:

```powershell
docker compose -p nephropaidi -f docker-compose.full.yml up -d --build ai-triage-service communication-service
Invoke-RestMethod http://127.0.0.1:8000/health
Invoke-RestMethod http://127.0.0.1:8085/actuator/health
```

After both are healthy, test the guardian create-message flow and confirm:

- message creation succeeds;
- existing queue is unchanged;
- AI metadata fields are populated when AI service is up;
- message creation still succeeds with `AI_TRIAGE_ENABLED=false` or with `ai-triage-service` stopped.

## Phase 2D - Microservice Structure Consistency and Docker Runtime Recovery

### Objective

Align the AI triage service with the backend microservice placement convention shown in the reference screenshots, then resume Docker runtime validation.

### Analysis and Reasoning

The local project already places application backend services under `BackEnd/microservices/`. The reference screenshots show the related Python AI backend service convention as `BackEnd/microservices/ai-clinical-service`. Since `ai-triage-service` is also a standalone backend microservice, the most coherent placement is:

```text
BackEnd/microservices/ai-triage-service
```

This preserves the intended boundary: the AI service remains independent from `communication-service`, owns no database state, and does not own routing or workflow behavior.

### Files Inspected

- `BackEnd/`
- `BackEnd/microservices/`
- `docker-compose.full.yml`
- `scripts/start-stack.ps1`
- `scripts/start-stack.sh`
- `README.md`
- `BackEnd/microservices/ai-triage-service/README.md`
- `BackEnd/microservices/ai-triage-service/training/export_model.py`
- `BackEnd/microservices/ai-triage-service/training/verify_artifact.py`
- `AI_TRIAGE_DEPLOYMENT_LOG.md`

### Files Modified

- `docker-compose.full.yml`
- `README.md`
- `BackEnd/microservices/ai-triage-service/README.md`
- `BackEnd/microservices/ai-triage-service/training/export_model.py`
- `BackEnd/microservices/ai-triage-service/training/verify_artifact.py`
- `AI_TRIAGE_DEPLOYMENT_LOG.md`

### Exact Changes Made

- Moved:
  - from `BackEnd/ai-triage-service`
  - to `BackEnd/microservices/ai-triage-service`
- Updated Docker build context:
  - `./BackEnd/microservices/ai-triage-service`
- Updated AI service README commands to use the new path.
- Updated training/export default artifact path to the new service path.
- Updated artifact verification default model path to the new service path.
- Added AI Triage Service URL to the root README service list.
- No changes were made to routing, queues, dashboards, receptionist/nurse/doctor logic, escalation, or frontend behavior.

### Why the Change Was Necessary

This avoids a mixed backend layout where one microservice sits outside the project’s microservice convention. It also makes future maintenance clearer because `ai-triage-service` sits beside `communication-service`, `clinical-service`, and the other backend services.

### Risks Identified

- Path references can break Docker builds or local model export commands if any old `BackEnd/ai-triage-service` reference remains.
- Docker runtime validation depends on Docker Desktop health and the existing stack services.
- Real message-creation validation still depends on available authentication credentials and a guardian/patient relationship in the current runtime data.

### Rollback Strategy

- Move `BackEnd/microservices/ai-triage-service` back to `BackEnd/ai-triage-service`.
- Revert the build context and documentation paths.
- No database rollback is needed for this structural move.
- No workflow rollback is needed because no workflow behavior changed.

### Current Status

- Structure move completed.
- Path updates completed.
- Docker/runtime validation continuing next in this same phase.

### Runtime Validation Commands and Results

Path/config reference check:

```powershell
rg -n "BackEnd[/\\]ai-triage-service|BackEnd/microservices/ai-triage-service|AI_TRIAGE" -S docker-compose.full.yml scripts README.md BackEnd\microservices\ai-triage-service BackEnd\config-server\src\main\resources\config\communication-service.yml
docker compose -p nephropaidi -f docker-compose.full.yml config --quiet
```

Result:

- Active service paths now point to `BackEnd/microservices/ai-triage-service`.
- Compose static validation passed.

Docker engine recovery:

```powershell
docker context ls
docker info
Start-Process -FilePath "C:\Program Files\Docker\Docker\Docker Desktop.exe" -WindowStyle Hidden
docker version
docker ps
```

Result:

- Docker Desktop initially lost the `dockerDesktopLinuxEngine` pipe.
- Docker Desktop was started again and the engine became reachable.

Build/start command attempted:

```powershell
docker compose -p nephropaidi -f docker-compose.full.yml up -d --build ai-triage-service communication-service
```

Result:

- `ai-triage-service` image built successfully from `BackEnd/microservices/ai-triage-service`.
- `communication-service` image built successfully.
- Compose hit existing container-name conflicts for already restored `nephro-*` containers.

Targeted container refresh:

```powershell
docker rm -f nephro-ai-triage-service nephro-communication-service
docker compose -p nephropaidi -f docker-compose.full.yml up -d --no-deps ai-triage-service communication-service
```

Result:

- `nephro-ai-triage-service` started on port `8000`.
- `nephro-communication-service` started on port `8085`.

Health checks:

```powershell
Invoke-RestMethod http://127.0.0.1:8000/health
Invoke-RestMethod http://127.0.0.1:8085/actuator/health
```

Result:

- AI service: `{"status":"UP","modelLoaded":true}`
- Communication service: `status=UP`
- Docker health eventually reached `healthy` for `nephro-ai-triage-service`.

Prediction checks:

```powershell
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8000/predict -ContentType "application/json" -Body '{...}'
```

Valid request examples returned stable response shape:

- HIGH example: `urgencyLevel=CRITICAL`, `confidence=0.9384`, `status=SUCCESS`, `modelVersion=followup-triage-safe-v1`
- CRITICAL example: `urgencyLevel=CRITICAL`, `confidence=0.9214`, `status=SUCCESS`, `modelVersion=followup-triage-safe-v1`
- LOW/MEDIUM examples initially failed with HTTP 422 because `guardianPriority` only accepts `NORMAL` or `HIGH`.
- Retried LOW/MEDIUM using `guardianPriority=NORMAL`; responses succeeded but over-predicted `CRITICAL`.

Network reachability:

```powershell
docker run --rm --network nephro-net curlimages/curl:8.10.1 -fsS http://ai-triage-service:8000/health
docker run --rm --network container:nephro-communication-service curlimages/curl:8.10.1 -v --max-time 10 http://ai-triage-service:8000/health
docker exec nephro-communication-service printenv AI_TRIAGE_SERVICE_BASE_URL
```

Result:

- Docker DNS resolved `ai-triage-service`.
- HTTP from the communication-service network namespace to AI `/health` returned 200.
- `AI_TRIAGE_SERVICE_BASE_URL=http://ai-triage-service:8000` was present in the communication-service container.

### Config Fix Identified During Runtime Validation

Problem:

- `communication-service.yml` is served by config-server.
- The placeholder `${AI_TRIAGE_SERVICE_BASE_URL:http://localhost:8000}` was being resolved by config-server, not by communication-service.
- Because config-server did not have `AI_TRIAGE_SERVICE_BASE_URL`, it served `http://localhost:8000`.
- Inside `communication-service`, `localhost:8000` points to the communication-service container itself, so AI calls failed with `ResourceAccessException`.

Fix:

- Added the AI triage environment variables to the `config-server` service in `docker-compose.full.yml`:
  - `AI_TRIAGE_ENABLED`
  - `AI_TRIAGE_SERVICE_BASE_URL`
  - `AI_TRIAGE_TIMEOUT_MILLIS`

Restart commands:

```powershell
docker rm -f nephro-config-server nephro-communication-service
docker compose -p nephropaidi -f docker-compose.full.yml up -d --no-deps config-server
docker compose -p nephropaidi -f docker-compose.full.yml up -d --no-deps communication-service
```

Result:

- Config-server health: `UP`
- Communication-service health: `UP`
- Both containers now expose/use `AI_TRIAGE_SERVICE_BASE_URL=http://ai-triage-service:8000`.

### Runtime Message Creation Validation

Because no reusable guardian password was available in the repository, a validation-only guardian and patient were created through existing services:

- Guardian username: `codextriage20260507114910`
- Guardian user id: `111`
- Guardian Keycloak id: `37385cef-5fa8-4fd7-b116-43004f0b9d88`
- Patient id: `23`

No code or workflow was changed for this test data.

AI unavailable/race-before-fix results:

- Message `65f447f8-939f-4e80-9cca-f1dc5e884914`
  - `status=PENDING`
  - `queue=NURSE`
  - `priority=HIGH`
  - `aiTriageStatus=FAILED`
- Message `706737b6-b594-4e09-80a8-4a46e3cc3806`
  - `status=PENDING`
  - `queue=NURSE`
  - `priority=HIGH`
  - `aiTriageStatus=FAILED`

AI available after config fix:

- Message `31915228-c7e3-4962-b648-163aa12623cf`
  - `status=PENDING`
  - `queue=NURSE`
  - `priority=HIGH`
  - `aiUrgencyLevel=CRITICAL`
  - `aiConfidence=0.9605`
  - `aiTriageStatus=SUCCESS`
  - `aiModelVersion=followup-triage-safe-v1`

AI unavailable fallback:

```powershell
docker stop nephro-ai-triage-service
```

- Message `3cbca75c-ab2e-4861-8d5a-982defa611b1`
  - `status=PENDING`
  - `queue=NURSE`
  - `priority=HIGH`
  - `aiTriageStatus=FAILED`
  - message creation succeeded even while AI was unavailable.

AI service was restored afterward:

```powershell
docker compose -p nephropaidi -f docker-compose.full.yml up -d --no-deps ai-triage-service
Invoke-RestMethod http://127.0.0.1:8000/health
```

Result:

- AI service restored to `{"status":"UP","modelLoaded":true}`.
- Docker health showed `nephro-ai-triage-service` as `healthy`.

### Isolated Test Validation

```powershell
docker run --rm -v "${PWD}:/work" -w /work/BackEnd/microservices/ai-triage-service python:3.12-slim sh -c "pip install --no-cache-dir -q -r requirements.txt -r requirements-dev.txt && pytest -q"
```

Result:

- `3 passed in 3.83s`

### Risks and Blockers

- The model currently over-predicts `CRITICAL` on some LOW/MEDIUM realistic examples. This is acceptable for advisory-only metadata validation, but it should be addressed before using AI output for routing or assignment.
- The AI API schema currently accepts `guardianPriority=NORMAL` or `HIGH`; validation calls using `LOW` or `MEDIUM` correctly return 422.
- Docker Desktop may restore containers in a way that causes Compose name conflicts. Targeted `docker rm -f` for the specific containers resolved this without changing application workflow.
- Validation-only data now exists in the running environment unless it is cleaned up later.

### Rollback Strategy

Configuration rollback:

- Remove `ai-triage-service` from `docker-compose.full.yml`.
- Remove `AI_TRIAGE_*` environment variables from both `config-server` and `communication-service` in `docker-compose.full.yml`.
- Recreate config-server and communication-service.

Operational rollback:

```powershell
$env:AI_TRIAGE_ENABLED="false"
docker compose -p nephropaidi -f docker-compose.full.yml up -d --no-deps config-server communication-service
```

Data cleanup rollback for validation-only data:

- Delete or soft-delete guardian `codextriage20260507114910`.
- Delete or ignore validation patient `23`.
- Delete or ignore validation follow-up messages listed above.

No route, queue, dashboard, role, or escalation rollback is required because none of those behaviors were changed.

### Final Status

- `ai-triage-service` placement is now consistent with backend microservice convention.
- Docker Compose uses the moved path.
- Config-server now publishes the correct AI service URL to communication-service.
- AI `/health` works.
- AI `/predict` works.
- communication-service can reach AI service by container name.
- Guardian message creation succeeds with AI available and stores advisory AI metadata.
- Guardian message creation succeeds with AI unavailable and stores failure metadata.
- Existing queue/routing remained unchanged in validation: HIGH medical messages still went to `NURSE`.
- AI remains advisory only.

## Phase 3A - Minimal Frontend Advisory Visibility

### Objective

Show stored AI triage metadata to staff users as advisory information only, without changing any workflow behavior.

### Analysis and Reasoning

The existing staff communication surfaces are:

- `FrontEnd/src/app/pages/backoffice/communication-inbox`
- `FrontEnd/src/app/pages/backoffice/communication-details`
- shared API model: `FrontEnd/src/app/core/services/communication-api.service.ts`

The safest UI change is to expose the already-stored metadata as simple badges/labels. No AI-based filtering, sorting, assignment, routing, escalation, or dashboard redesign was added.

### Files Inspected

- `FrontEnd/src/app/core/services/communication-api.service.ts`
- `FrontEnd/src/app/pages/backoffice/communication-inbox/communication-inbox.ts`
- `FrontEnd/src/app/pages/backoffice/communication-inbox/communication-inbox.html`
- `FrontEnd/src/app/pages/backoffice/communication-inbox/communication-inbox.scss`
- `FrontEnd/src/app/pages/backoffice/communication-details/communication-details.ts`
- `FrontEnd/src/app/pages/backoffice/communication-details/communication-details.html`
- `FrontEnd/src/app/pages/backoffice/communication-details/communication-details.scss`
- `FrontEnd/package.json`

### Files Modified

- `FrontEnd/src/app/core/services/communication-api.service.ts`
- `FrontEnd/src/app/pages/backoffice/communication-inbox/communication-inbox.ts`
- `FrontEnd/src/app/pages/backoffice/communication-inbox/communication-inbox.html`
- `FrontEnd/src/app/pages/backoffice/communication-details/communication-details.ts`
- `FrontEnd/src/app/pages/backoffice/communication-details/communication-details.html`
- `AI_TRIAGE_DEPLOYMENT_LOG.md`

### Exact Changes Made

- Added frontend API model types:
  - `AiUrgencyLevel`
  - `AiTriageStatus`
- Added optional AI metadata fields to `FollowUpMessage`:
  - `aiUrgencyLevel`
  - `aiConfidence`
  - `aiTriageStatus`
  - `aiExplanation`
  - `aiModelVersion`
  - `aiEvaluatedAt`
- Added an `AI Advisory` column in the staff inbox table:
  - AI status badge
  - AI urgency badge when available
  - confidence percentage when available
- Added an `AI Triage Advisory` card in conversation details:
  - AI status
  - urgency
  - confidence
  - explanation
  - model version
  - evaluation time
- Added small helper methods for badge classes and confidence formatting.

### Why the Change Was Necessary

The backend now stores advisory AI triage metadata. Staff need a minimal way to see that information during review without the AI controlling workflow decisions.

### Risks Identified

- The model currently over-predicts `CRITICAL` for some realistic low/medium text examples. Keeping the UI advisory-only avoids workflow risk.
- Adding AI filters or sorting could make staff behavior depend on model output, so those were intentionally not added.
- Screenshots were not captured in this phase because the available browser automation control surface was not available, and creating extra staff/escalation runtime data only for screenshots would have widened the phase.

### Validation

Frontend build command:

```powershell
cd FrontEnd
npm run build
```

Result:

- Build succeeded.
- Existing bundle budget warnings remained:
  - initial bundle budget exceeded
  - several pre-existing component style budgets exceeded
- No Angular template/type errors were introduced by this AI advisory display.

### Rollback Strategy

- Remove the AI metadata type fields from `communication-api.service.ts`.
- Remove the `AI Advisory` column from `communication-inbox.html`.
- Remove the AI helper methods from `communication-inbox.ts`.
- Remove the `AI Triage Advisory` card from `communication-details.html`.
- Remove the AI helper methods from `communication-details.ts`.

No backend rollback, routing rollback, queue rollback, role rollback, escalation rollback, or dashboard rollback is needed for this frontend-only advisory display.

### Current Status

- Minimal staff-facing AI metadata visibility is implemented.
- No routing changes.
- No queue changes.
- No role changes.
- No escalation changes.
- No dashboard redesign.
- AI remains advisory only.

### Next Recommendation

Keep the next phase limited to review and polish only, or pause for manual UI inspection before adding any AI-based filter/sort behavior.

## Final Consolidation and Verification Checkpoint

### Objective

Stop feature development and consolidate the AI triage integration status, changed files, validation results, demo steps, rollback notes, and remaining risks.

### Final Git Status Summary

The working tree contains the AI triage integration changes only. Files are grouped below by responsibility.

#### AI Service

- `BackEnd/microservices/ai-triage-service/.gitignore`
- `BackEnd/microservices/ai-triage-service/Dockerfile`
- `BackEnd/microservices/ai-triage-service/README.md`
- `BackEnd/microservices/ai-triage-service/app/__init__.py`
- `BackEnd/microservices/ai-triage-service/app/main.py`
- `BackEnd/microservices/ai-triage-service/app/model_loader.py`
- `BackEnd/microservices/ai-triage-service/app/pipeline.py`
- `BackEnd/microservices/ai-triage-service/app/schemas.py`
- `BackEnd/microservices/ai-triage-service/models/followup_triage.joblib`
- `BackEnd/microservices/ai-triage-service/models/followup_triage_metadata.json`
- `BackEnd/microservices/ai-triage-service/requirements-dev.txt`
- `BackEnd/microservices/ai-triage-service/requirements.txt`
- `BackEnd/microservices/ai-triage-service/tests/test_api.py`
- `BackEnd/microservices/ai-triage-service/training/export_model.py`
- `BackEnd/microservices/ai-triage-service/training/verify_artifact.py`

#### Communication-Service Backend

- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/ai/AiTriageClient.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/ai/AiTriageProperties.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/ai/AiTriageRequest.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/ai/AiTriageResponse.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/ai/AiTriageResult.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/domain/entity/FollowUpMessage.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/domain/enums/AiTriageStatus.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/domain/enums/AiUrgencyLevel.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/dto/response/CreateMessageResponse.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/dto/response/FollowUpMessageResponse.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/mapper/MessageMapper.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/service/impl/FollowUpMessageServiceImpl.java`
- `BackEnd/microservices/communication-service/src/main/resources/db/migration/V6__add_ai_triage_metadata_to_follow_up_messages.sql`
- `BackEnd/microservices/communication-service/src/test/java/tn/esprit/spring/communicationservice/service/impl/FollowUpMessageServiceImplTest.java`

#### Frontend Visibility

- `FrontEnd/src/app/core/services/communication-api.service.ts`
- `FrontEnd/src/app/pages/backoffice/communication-details/communication-details.html`
- `FrontEnd/src/app/pages/backoffice/communication-details/communication-details.ts`
- `FrontEnd/src/app/pages/backoffice/communication-inbox/communication-inbox.html`
- `FrontEnd/src/app/pages/backoffice/communication-inbox/communication-inbox.ts`

#### Docker, Config, and Scripts

- `BackEnd/config-server/src/main/resources/config/communication-service.yml`
- `docker-compose.full.yml`
- `scripts/start-stack.ps1`
- `scripts/start-stack.sh`

#### Documentation

- `AI_TRIAGE_DEPLOYMENT_LOG.md`
- `README.md`

### Behavior Confirmation

- No routing logic changed.
- No queue decision logic changed.
- No role logic changed.
- No nurse-to-doctor escalation logic changed.
- No dashboard behavior was redesigned.
- AI remains advisory only.
- The existing `routeQueue(...)` behavior remains the source of assignment queue decisions.
- Frontend displays AI metadata but does not use AI for filtering, sorting, assignment, or escalation.

### Final Validation Results

Git status:

```powershell
git status --short
git status
```

Result:

- Branch: `Malek`
- Branch is up to date with `origin/Malek`
- Changes are unstaged/untracked and match the grouped file list above.

Communication-service full test attempt:

```powershell
cd BackEnd/microservices/communication-service
mvn -q test
```

Result:

- Failed because context-level tests attempted to load config from `http://localhost:8888/communication-service/default` while local config-server was not available at the start of the test run.
- Primary error: `ConfigClientFailFastException` / connection refused to config-server.
- This is an environment/config-server dependency issue in the full suite, not an AI compile error.

Focused backend validation:

```powershell
cd BackEnd/microservices/communication-service
mvn -q -DskipTests compile
mvn -q -Dtest=FollowUpMessageServiceImplTest test
```

Result:

- Passed.

Frontend build:

```powershell
cd FrontEnd
npm run build
```

Result:

- Build succeeded.
- Existing Angular budget warnings remain:
  - initial bundle budget exceeded
  - several pre-existing component style budgets exceeded.

AI artifact verification:

```powershell
cd BackEnd/microservices/ai-triage-service
python -m compileall -q app training tests
python training\verify_artifact.py --model models\followup_triage.joblib
```

Result:

- Python files compiled.
- Artifact loaded and predicted `CRITICAL` with confidence `0.8384`.

AI isolated tests through Docker:

```powershell
docker run --rm -v "${PWD}:/work" -w /work/BackEnd/microservices/ai-triage-service python:3.12-slim sh -c "pip install --no-cache-dir -q -r requirements.txt -r requirements-dev.txt && pytest -q"
```

Result:

- `3 passed in 9.17s`

Docker Compose config:

```powershell
docker compose -p nephropaidi -f docker-compose.full.yml config --quiet
```

Result:

- Passed.

AI service Docker health and predict:

```powershell
Invoke-RestMethod http://127.0.0.1:8000/health
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8000/predict -ContentType "application/json" -Body '{"messageText":"Fievre a 39 avec vomissements depuis hier soir apres la dialyse.","subject":"Fievre et vomissements","messageType":"MEDICAL","guardianPriority":"HIGH"}'
```

Result:

- Health: `{"status":"UP","modelLoaded":true}`
- Predict: `urgencyLevel=CRITICAL`, `confidence=0.9384`, `status=SUCCESS`, `modelVersion=followup-triage-safe-v1`
- Docker showed `nephro-ai-triage-service` as `healthy` with `0.0.0.0:8000->8000/tcp`.

### Demo Steps

Start the stack:

```powershell
.\scripts\start-stack.ps1
```

Or manually:

```powershell
docker compose -p nephropaidi -f docker-compose.full.yml up -d --build
```

Confirm AI service:

```powershell
Invoke-RestMethod http://127.0.0.1:8000/health
```

Create a guardian follow-up message through the app:

1. Log in as a guardian.
2. Open the guardian communication page.
3. Create a new follow-up message.
4. Use a realistic medical message, for example: `Fievre a 39 avec vomissements depuis hier soir apres la dialyse.`

Verify AI metadata is stored:

1. Inspect the create-message API response, or fetch the message details through `/api/communication/messages/{id}`.
2. Expected advisory fields when AI is available:
   - `aiUrgencyLevel`
   - `aiConfidence`
   - `aiTriageStatus=SUCCESS`
   - `aiExplanation`
   - `aiModelVersion`
   - `aiEvaluatedAt`

Open staff inbox/detail:

1. Log in as `RECEPTIONIST`, `NURSE`, or `DOCTOR`.
2. Open `/backoffice/communication/inbox`.
3. Confirm the new `AI Advisory` column shows AI status, urgency, and confidence.
4. Open the message detail page.
5. Confirm the `AI Triage Advisory` card shows the stored metadata.

Test fallback behavior:

```powershell
docker stop nephro-ai-triage-service
```

Then create another guardian message. Expected result:

- Message creation still succeeds.
- Existing queue/routing remains unchanged.
- AI metadata records a failure status, usually `aiTriageStatus=FAILED`.

Restore AI service:

```powershell
docker compose -p nephropaidi -f docker-compose.full.yml up -d --no-deps ai-triage-service
Invoke-RestMethod http://127.0.0.1:8000/health
```

### Remaining Risks and Blockers

- The safe model currently over-predicts `CRITICAL` on some LOW/MEDIUM-style examples. Keep it advisory until model quality is improved.
- The model was trained from the available CSV using safe creation-time features only. Reported accuracy should be interpreted carefully because the data may be synthetic or highly separable.
- Full communication-service `mvn test` can fail when config-server is not running because some context tests import remote config. Focused compile and AI integration tests pass.
- Docker Desktop can intermittently lose the Linux engine pipe on this machine; restarting Docker Desktop restores it.
- Validation-only runtime data may still exist in the remote databases from earlier tests.
- The AI endpoint contract currently accepts `guardianPriority=NORMAL` or `HIGH`; invalid values such as `LOW` or `MEDIUM` return HTTP 422 as expected.

### Final Status

- AI triage microservice is implemented under `BackEnd/microservices/ai-triage-service`.
- communication-service stores AI metadata on follow-up messages.
- communication-service calls AI during guardian message creation and fails open if AI is down.
- Frontend staff inbox/details display stored AI metadata as advisory labels.
- Docker Compose/config/scripts include the AI service.
- No routing, queue, role, escalation, or dashboard workflow behavior was changed.

## Recovery Audit on Branch `Malek`

### Objective

Restore the completed advisory AI triage integration after recovering from the orphan branch state and switching back to the real `Malek` branch.

### Analysis and Reasoning

`git diff --name-status` was empty on `Malek`, while `git status --short` showed only untracked AI files. This confirmed that the tracked integration edits had disappeared from the active branch, but the isolated AI service, AI client package, enums, migration, and deployment log were still present as untracked files.

### Files Inspected

- `docker-compose.full.yml`
- `scripts/start-stack.ps1`
- `scripts/start-stack.sh`
- `README.md`
- `BackEnd/config-server/src/main/resources/config/communication-service.yml`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/domain/entity/FollowUpMessage.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/dto/response/CreateMessageResponse.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/dto/response/FollowUpMessageResponse.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/mapper/MessageMapper.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/service/impl/FollowUpMessageServiceImpl.java`
- `BackEnd/microservices/communication-service/src/test/java/tn/esprit/spring/communicationservice/service/impl/FollowUpMessageServiceImplTest.java`
- `FrontEnd/src/app/core/services/communication-api.service.ts`
- `FrontEnd/src/app/pages/backoffice/communication-inbox/*`
- `FrontEnd/src/app/pages/backoffice/communication-details/*`

### Files Modified

- Reapplied AI metadata persistence fields, mapper fields, DTO fields, and fail-open AI call in `communication-service`.
- Reapplied frontend advisory-only display in staff inbox and message details.
- Reapplied Docker/config/script references for `ai-triage-service`.
- Kept the AI service separate under `BackEnd/microservices/ai-triage-service`.

### Exact Changes Made

- Added `aiUrgencyLevel`, `aiConfidence`, `aiTriageStatus`, `aiExplanation`, `aiModelVersion`, and `aiEvaluatedAt` to follow-up message persistence and API responses.
- Restored the internal `AiTriageClient` use during guardian message creation.
- Restored fail-open behavior: message creation continues if AI is disabled, unavailable, or fails.
- Restored advisory frontend badges/card without sorting, filtering, assignment, or queue changes.
- Restored Docker Compose service entry and runtime environment variables.

### Validation

```powershell
git diff --name-status
git diff --check
python BackEnd\microservices\ai-triage-service\training\verify_artifact.py
docker compose -p nephropaidi -f docker-compose.full.yml config --quiet
cd BackEnd\microservices\communication-service
.\mvnw.cmd -q -DskipTests compile
.\mvnw.cmd -q -Dtest=FollowUpMessageServiceImplTest test
cd ..\..\..\FrontEnd
npm run build
```

Results:

- `git diff --check`: no whitespace errors; only CRLF normalization warnings.
- AI artifact verification: returned a real prediction (`CRITICAL`, confidence `0.8384`).
- Docker Compose config check: passed.
- communication-service compile: passed.
- `FollowUpMessageServiceImplTest`: passed.
- frontend build: passed with existing bundle budget warnings unrelated to AI triage.
- Docker AI smoke test: `ai-triage-service` built and became healthy; `/health` returned `UP` with `modelLoaded=true`; `/predict` returned `CRITICAL`, confidence `0.7613`, status `SUCCESS`.

### Risks Identified

- Untracked Python cache folders exist under the AI service and should not be staged.
- The model remains advisory only because quality should be improved before any workflow automation.
- Full-stack runtime validation still depends on Docker/remote database availability.

### Rollback Strategy

- Remove the AI service from Docker Compose/scripts/config.
- Remove communication-service AI fields/client call/migration/DTO mapper changes.
- Remove frontend advisory-only display fields.
- Keep or delete `AI_TRIAGE_DEPLOYMENT_LOG.md` according to project documentation preference.

### Current Status

Recovered on branch `Malek`. AI triage integration is restored as advisory metadata only. No routing, queue, role logic, escalation, or dashboard behavior was changed.

### Next Recommendation

Stage only the AI triage integration files and avoid staging generated Python cache files.

## UI Cleanup: AI Prediction as Visible Triage Classification

### Objective

Make the UI use the AI prediction as the visible triage classification and hide the temporary guardian manual priority controls that could confuse staff and guardians.

### Analysis and Reasoning

The backend still requires `priority` for existing message creation and routing compatibility. To keep the change reversible and avoid workflow risk, the frontend now keeps sending the default `NORMAL` priority silently while removing manual priority visibility from the guardian form and staff-facing triage display areas.

### Files Inspected

- `FrontEnd/src/app/pages/frontoffice/communication-new/communication-new.ts`
- `FrontEnd/src/app/pages/frontoffice/communication-new/communication-new.html`
- `FrontEnd/src/app/pages/frontoffice/communication-list/communication-list.html`
- `FrontEnd/src/app/pages/frontoffice/communication-thread/communication-thread.html`
- `FrontEnd/src/app/pages/backoffice/communication-inbox/communication-inbox.html`
- `FrontEnd/src/app/pages/backoffice/communication-details/communication-details.html`

### Files Modified

- `FrontEnd/src/app/pages/frontoffice/communication-new/communication-new.ts`
- `FrontEnd/src/app/pages/frontoffice/communication-new/communication-new.html`
- `FrontEnd/src/app/pages/frontoffice/communication-list/communication-list.html`
- `FrontEnd/src/app/pages/frontoffice/communication-thread/communication-thread.html`
- `FrontEnd/src/app/pages/backoffice/communication-inbox/communication-inbox.html`
- `FrontEnd/src/app/pages/backoffice/communication-details/communication-details.html`

### Exact Changes Made

- Removed the guardian `Mark as Urgent` checkbox from the new message form.
- Kept the hidden/default create-message priority value as `NORMAL` so existing backend validation and routing continue to work.
- Removed the old manual `Priority` column from the staff inbox table.
- Removed the old priority badge from staff message details.
- Removed old urgent badges from guardian message list/thread views.
- Kept `AI Advisory` visible as the main staff triage display: AI urgency level, confidence, and status.

### Why the Change Was Necessary

The manual guardian priority was a temporary pre-AI signal. Leaving it visible beside the AI prediction makes it look like the application has two competing triage classifications.

### Risks Identified

- Existing old messages may still have `priority=HIGH` in the database, but that value is no longer shown as the visible triage classification.
- Backend routing still uses the legacy `priority` field internally until an approved architecture phase changes routing behavior.

### Rollback Strategy

- Restore the removed priority checkbox and priority badges/column in the touched frontend templates.
- Restore the removed helper methods in `communication-new.ts` if the checkbox is brought back.

### Current Status

Small reversible frontend cleanup completed. No backend routing, queue, role, escalation, database, or workflow behavior was changed.

### Validation

```powershell
cd FrontEnd
npm run build
npx ng build --configuration development
```

Results:

- First `npm run build` attempt did not complete because Angular tried to inline Google Fonts and DNS lookup failed for `fonts.googleapis.com` (`getaddrinfo ENOTFOUND`). This was an external network/font-inlining issue, not a TypeScript or template error.
- `npx ng build --configuration development` passed and generated the frontend bundle successfully.
- Retried `npm run build` after DNS recovered; production build passed with existing bundle budget warnings unrelated to this UI cleanup.

### Next Recommendation

Keep AI as the visible triage classification for the demo. Consider a later approved backend phase if routing should eventually use AI urgency instead of legacy priority.

## Workflow Cleanup: Medical AI vs Non-Medical Manual Urgency

### Objective

Clean up the triage workflow so `MEDICAL` messages use AI as the only visible triage classification, while non-medical messages keep manual operational urgency without showing AI badges or skipped states.

### Analysis and Reasoning

Two different urgency systems were still coexisting:

- Manual guardian priority (`NORMAL` / `HIGH`)
- AI triage urgency (`LOW` / `MEDIUM` / `HIGH` / `CRITICAL`)

This is acceptable internally for backward compatibility, but confusing if both are shown together. The safe solution was:

- keep backend `priority` support unchanged for compatibility and routing stability;
- run AI only for `MEDICAL`;
- mark non-medical AI as `SKIPPED` internally;
- hide skipped AI completely in the UI.

### Files Inspected

- `FrontEnd/src/app/pages/frontoffice/communication-new/communication-new.ts`
- `FrontEnd/src/app/pages/frontoffice/communication-new/communication-new.html`
- `FrontEnd/src/app/pages/backoffice/communication-inbox/communication-inbox.ts`
- `FrontEnd/src/app/pages/backoffice/communication-inbox/communication-inbox.html`
- `FrontEnd/src/app/pages/backoffice/communication-details/communication-details.ts`
- `FrontEnd/src/app/pages/backoffice/communication-details/communication-details.html`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/service/impl/FollowUpMessageServiceImpl.java`
- `BackEnd/microservices/communication-service/src/test/java/tn/esprit/spring/communicationservice/service/impl/FollowUpMessageServiceImplTest.java`

### Files Modified

- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/service/impl/FollowUpMessageServiceImpl.java`
- `BackEnd/microservices/communication-service/src/test/java/tn/esprit/spring/communicationservice/service/impl/FollowUpMessageServiceImplTest.java`
- `FrontEnd/src/app/pages/frontoffice/communication-new/communication-new.ts`
- `FrontEnd/src/app/pages/frontoffice/communication-new/communication-new.html`
- `FrontEnd/src/app/pages/backoffice/communication-inbox/communication-inbox.ts`
- `FrontEnd/src/app/pages/backoffice/communication-inbox/communication-inbox.html`
- `FrontEnd/src/app/pages/backoffice/communication-details/communication-details.ts`
- `FrontEnd/src/app/pages/backoffice/communication-details/communication-details.html`

### Exact Changes Made

- Guardian new-message form:
  - shows manual urgent checkbox only when the selected type is not `MEDICAL`;
  - hides it for `MEDICAL`;
  - resets priority to `NORMAL` whenever the type switches to `MEDICAL`.
- communication-service:
  - runs AI only for `MEDICAL`;
  - sets AI metadata to `SKIPPED` with null advisory fields for non-medical messages;
  - keeps existing routing and message creation behavior unchanged.
- Staff inbox:
  - shows AI advisory only when the message is `MEDICAL` and AI actually ran or failed;
  - shows an empty/clean AI cell for intentionally skipped non-medical messages.
- Staff details:
  - shows the AI advisory card only for relevant medical messages;
  - hides the card for intentionally skipped non-medical messages.

### Risks Identified

- Legacy inbox tabs, sorting, and filters still rely partly on the old backend `priority` field. This phase intentionally did not redesign that behavior to avoid workflow drift.
- Medical messages now default to `NORMAL` priority from the guardian form, so legacy priority-based inbox behavior is no longer a reliable proxy for medical urgency. The AI advisory is the intended visible triage signal.

### Rollback Strategy

- Restore unconditional AI execution in `FollowUpMessageServiceImpl`.
- Restore always-visible AI advisory blocks in staff inbox/details.
- Restore the always-hidden or always-visible manual urgent checkbox behavior in the guardian form.

### Validation

```powershell
cd BackEnd\microservices\communication-service
.\mvnw.cmd -q -Dtest=FollowUpMessageServiceImplTest test

cd ..\..\..\FrontEnd
npm run build
```

Results:

- Focused backend test passed.
- Frontend production build passed with existing bundle budget warnings unrelated to this workflow cleanup.

### Current Status

- `MEDICAL` messages use AI triage as the visible advisory classification.
- Non-medical messages keep manual urgency without AI clutter in the UI.
- Routing, queues, assignment, role logic, escalation, and database structure were not changed.

## UI Cleanup: Prediction Class vs Execution Status

### Objective

Refine the staff UI so the visible AI triage result is the prediction class only (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`), while technical execution states such as `SUCCESS` remain internal.

### Analysis and Reasoning

The previous UI still exposed `SUCCESS` as a badge in inbox/details, which made the execution status look like a fifth triage class. That was misleading. The cleaner rule is:

- successful medical AI run: show prediction class and confidence;
- failed medical AI run: show a compact warning;
- receptionist workflow: hide the AI column entirely;
- non-medical skipped cases: show nothing AI-related.

### Files Modified

- `FrontEnd/src/app/pages/backoffice/communication-inbox/communication-inbox.ts`
- `FrontEnd/src/app/pages/backoffice/communication-inbox/communication-inbox.html`
- `FrontEnd/src/app/pages/backoffice/communication-details/communication-details.ts`
- `FrontEnd/src/app/pages/backoffice/communication-details/communication-details.html`

### Exact Changes Made

- Staff inbox:
  - removed visible `SUCCESS` badge;
  - shows only urgency class badge plus confidence for successful medical AI predictions;
  - shows a small `AI triage unavailable` warning only for failed medical AI runs;
  - hides the entire AI column for receptionist.
- Staff details:
  - removed visible `SUCCESS` badge;
  - shows urgency class, confidence, model version, evaluated timestamp, and explanation when relevant;
  - shows a compact warning when medical AI failed;
  - hides the card entirely when AI is irrelevant or intentionally skipped.

### Validation

```powershell
cd FrontEnd
npm run build
```

Results:

- Frontend production build passed.
- Existing Angular bundle-budget warnings remain unrelated to this AI display cleanup.

### Current Status

- Visible AI classification now matches the real model classes.
- Technical execution status is no longer shown as a false triage class.
- Receptionist workflow no longer includes an empty AI triage column.

