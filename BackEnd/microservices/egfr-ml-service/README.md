# eGFR ML Service (PiDev)

Run locally:

```powershell
cd BackEnd\microservices\egfr-ml-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8092 --reload
```

Endpoints:
- GET `/health`
- GET `/metadata`
- POST `/predict/regression`
- POST `/predict/classification?threshold=0.5`

Quick test:

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://127.0.0.1:8092/predict/classification?threshold=0.5" `
  -ContentType "application/json" `
  -Body (Get-Content .\sample_request.json -Raw)
```
