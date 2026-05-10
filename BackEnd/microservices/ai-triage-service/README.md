# NephroPaidi AI Triage Service

Isolated FastAPI service for guardian follow-up message urgency prediction.

This service does not access the NephroPaidi database and does not own message workflow state.

## Run Locally

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

Prediction example:

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri http://127.0.0.1:8000/predict `
  -ContentType "application/json" `
  -Body '{"messageText":"Fievre a 39 et vomissements depuis hier soir.","subject":"Symptomes","messageType":"MEDICAL","guardianPriority":"HIGH"}'
```

## Run With Docker

```powershell
docker build -t nephropaidi-ai-triage BackEnd/microservices/ai-triage-service
docker run --rm -p 8000:8000 nephropaidi-ai-triage
```

The Docker image copies `models/followup_triage.joblib` into `/app/models/followup_triage.joblib`.

## Run Tests

Recommended runtime: Python 3.12.

```powershell
cd BackEnd/microservices/ai-triage-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt -r requirements-dev.txt
pytest
```

## Artifact Verification Without FastAPI

This only requires the ML dependencies:

```powershell
python BackEnd/microservices/ai-triage-service/training/verify_artifact.py --model BackEnd/microservices/ai-triage-service/models/followup_triage.joblib
```

