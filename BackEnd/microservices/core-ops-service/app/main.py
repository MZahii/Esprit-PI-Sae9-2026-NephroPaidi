from __future__ import annotations

import json
import os
from datetime import datetime, timezone
from typing import Any
from urllib.parse import urlparse

import pika
import psycopg
from fastapi import FastAPI, HTTPException, Request
from fastapi.openapi.docs import get_swagger_ui_html
from pydantic import BaseModel, Field

from .ai_biomarker_extraction.main import (
    app as biomarker_extraction_app,
    infer_lab_result,
    predict as biomarker_predict,
)
from .ai_biomarker_extraction.schemas import (
    AIPredictionRequest,
    AIPredictionResponse,
    AiLabAnalysisRequest,
    AiLabAnalysisResponse,
)
from .ai_message_urgency_triage.main import app as triage_app
from .ai_pediatric_dose_safety.main import (
    DoseVerificationRequest,
    DoseVerificationResponse,
    app as pediatric_dose_safety_app,
    verify_dose,
)
from .ai_renal_function_forecasting.main import (
    app as renal_function_forecasting_app,
    metadata as renal_metadata,
    monitoring as renal_monitoring,
    predict_classification_endpoint,
    predict_regression_endpoint,
    startup as renal_startup,
)
from .ai_renal_function_forecasting.schemas import (
    ClassificationResponse,
    MetadataResponse,
    MonitoringResponse,
    PredictRequest,
    RegressionResponse,
)
from .ai_surgical_risk_prediction.main import app as surgical_risk_prediction_app


app = FastAPI(
    title="Core Ops Service",
    description="Unified operational and AI service for NephroPaidi.",
    version="0.2.0",
    docs_url=None,
    redoc_url=None,
    openapi_url="/v3/api-docs",
)

app.mount("/api/ai/message-urgency-triage", triage_app)
app.mount("/api/ai/biomarker-extraction", biomarker_extraction_app)
app.mount("/api/ai/pediatric-dose-safety", pediatric_dose_safety_app)
app.mount("/api/ai/renal-function-forecasting", renal_function_forecasting_app)
app.mount("/api/ai/surgical-risk-prediction", surgical_risk_prediction_app)


@app.get("/swagger-ui/index.html", include_in_schema=False)
@app.get("/docs", include_in_schema=False)
def swagger_ui(request: Request) -> Any:
    openapi_url = request.query_params.get("url", "/v3/api-docs/core-ops-service")
    return get_swagger_ui_html(
        openapi_url=openapi_url,
        title="Core Ops Service - Swagger UI",
        oauth2_redirect_url=str(request.base_url) + "docs/oauth2-redirect",
    )


@app.get("/v3/api-docs/core-ops-service", include_in_schema=False)
def core_ops_openapi_alias() -> Any:
    return app.openapi()


@app.on_event("startup")
def startup() -> None:
    renal_startup()


class PublishMessageRequest(BaseModel):
    exchange: str = Field(default="core.ops.exchange")
    routing_key: str = Field(default="core.ops.event")
    payload: dict[str, Any] | str
    persistent: bool = Field(default=True)


class BindQueueRequest(BaseModel):
    exchange: str = Field(default="core.ops.exchange")
    queue: str = Field(default="core.ops.queue")
    routing_key: str = Field(default="core.ops.#")


class ConsumeOnceRequest(BaseModel):
    queue: str = Field(default="core.ops.queue")
    auto_ack: bool = Field(default=False)


def _db_ping() -> bool:
    dsn = os.getenv("CORE_OPS_DB_URL")
    if not dsn:
        return False

    try:
        with psycopg.connect(dsn, connect_timeout=3) as conn:
            with conn.cursor() as cur:
                cur.execute("SELECT 1")
                _ = cur.fetchone()
        return True
    except Exception:
        return False


def _rabbitmq_url() -> str:
    return os.getenv("RABBITMQ_URL", "amqp://admin:admin123@rabbitmq:5672/%2F")


def _rabbitmq_display_target() -> str:
    parsed = urlparse(_rabbitmq_url())
    host = parsed.hostname or "rabbitmq"
    port = parsed.port or 5672
    vhost = parsed.path.lstrip("/") or "%2F"
    return f"{host}:{port}/{vhost}"


def _rabbitmq_connection() -> pika.BlockingConnection:
    params = pika.URLParameters(_rabbitmq_url())
    params.socket_timeout = 3
    params.connection_attempts = 2
    params.retry_delay = 1
    return pika.BlockingConnection(params)


def _rabbitmq_ping() -> bool:
    try:
        conn = _rabbitmq_connection()
        conn.close()
        return True
    except Exception:
        return False


@app.get("/actuator/health")
def actuator_health() -> dict[str, str]:
    db_ok = _db_ping()
    mq_ok = _rabbitmq_ping()
    if db_ok and mq_ok:
        return {"status": "UP"}
    return {"status": "DEGRADED"}


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "UP"}


@app.get("/api/core-ops/health")
def service_health() -> dict[str, str]:
    return {
        "service": os.getenv("SERVICE_NAME", "core-ops-service"),
        "status": "UP",
        "time": datetime.now(timezone.utc).isoformat(),
    }


@app.get("/api/core-ops/python-rationale")
def python_rationale() -> dict[str, list[str]]:
    return {
        "why_python": [
            "Faster API iteration for workflow-heavy operational tooling",
            "Strong ecosystem for analytics and optimization in staffing and scheduling",
            "Simple integration path for ML-assisted operational and clinical decision support",
            "Low ceremony for automation jobs and internal service orchestration",
        ]
    }


@app.get("/api/core-ops/ai/catalog")
def ai_catalog() -> dict[str, list[dict[str, object]]]:
    return {
        "service": "core-ops-service",
        "functions": [
            {
                "name": "AI Message Urgency Triage",
                "prefix": "/api/ai/message-urgency-triage",
                "input": "Guardian messages (text, subject, type)",
                "output": "LOW / MEDIUM / HIGH / CRITICAL",
                "businessImpact": "Faster response to urgent cases",
                "implemented": True,
            },
            {
                "name": "AI Biomarker Extraction",
                "prefix": "/api/ai/biomarker-extraction",
                "input": "OCR lab reports (noisy scans)",
                "output": "Biomarkers extracted + triage class",
                "businessImpact": "Less manual data entry",
                "implemented": True,
            },
            {
                "name": "AI Pediatric Dose Safety",
                "prefix": "/api/ai/pediatric-dose-safety",
                "input": "Prescriptions (dose, weight, age)",
                "output": "Normal / Underdose / Overdose",
                "businessImpact": "Safer medication administration",
                "implemented": True,
            },
            {
                "name": "AI Surgical Risk Prediction",
                "prefix": "/api/ai/surgical-risk-prediction",
                "input": "Pre-op / post-op clinical data",
                "output": "Complication risk (binary 0/1)",
                "businessImpact": "Earlier clinical intervention",
                "implemented": False,
                "note": "No trained model artifact currently exists in the repository.",
            },
            {
                "name": "AI Renal Function Forecasting",
                "prefix": "/api/ai/renal-function-forecasting",
                "input": "Clinical and lab history",
                "output": "eGFR 3/6/12 months + decline risk flag",
                "businessImpact": "Proactive renal follow-up",
                "implemented": True,
            },
        ],
    }


@app.get("/api/core-ops/rabbitmq/health")
@app.get("/api/rabbitmq/health")
def rabbitmq_health() -> dict[str, str]:
    is_up = _rabbitmq_ping()
    return {
        "component": "rabbitmq",
        "target": _rabbitmq_display_target(),
        "status": "UP" if is_up else "DOWN",
    }


@app.post("/api/core-ops/rabbitmq/bind-queue")
@app.post("/api/rabbitmq/bind-queue")
def bind_queue(req: BindQueueRequest) -> dict[str, str]:
    try:
        conn = _rabbitmq_connection()
        channel = conn.channel()
        channel.exchange_declare(exchange=req.exchange, exchange_type="topic", durable=True)
        channel.queue_declare(queue=req.queue, durable=True)
        channel.queue_bind(exchange=req.exchange, queue=req.queue, routing_key=req.routing_key)
        conn.close()
        return {
            "status": "BOUND",
            "exchange": req.exchange,
            "queue": req.queue,
            "routing_key": req.routing_key,
        }
    except Exception as exc:
        raise HTTPException(status_code=503, detail=f"RabbitMQ bind failed: {exc}") from exc


@app.post("/api/core-ops/rabbitmq/publish")
@app.post("/api/rabbitmq/publish")
def publish_message(req: PublishMessageRequest) -> dict[str, str]:
    try:
        conn = _rabbitmq_connection()
        channel = conn.channel()
        channel.exchange_declare(exchange=req.exchange, exchange_type="topic", durable=True)

        if isinstance(req.payload, str):
            body = req.payload
            content_type = "text/plain"
        else:
            body = json.dumps(req.payload)
            content_type = "application/json"

        channel.basic_publish(
            exchange=req.exchange,
            routing_key=req.routing_key,
            body=body.encode("utf-8"),
            properties=pika.BasicProperties(
                content_type=content_type,
                delivery_mode=2 if req.persistent else 1,
                timestamp=int(datetime.now(timezone.utc).timestamp()),
            ),
        )
        conn.close()
        return {
            "status": "PUBLISHED",
            "exchange": req.exchange,
            "routing_key": req.routing_key,
            "published_at": datetime.now(timezone.utc).isoformat(),
        }
    except Exception as exc:
        raise HTTPException(status_code=503, detail=f"RabbitMQ publish failed: {exc}") from exc


@app.post("/api/core-ops/rabbitmq/consume-once")
@app.post("/api/rabbitmq/consume-once")
def consume_once(req: ConsumeOnceRequest) -> dict[str, Any]:
    try:
        conn = _rabbitmq_connection()
        channel = conn.channel()
        channel.queue_declare(queue=req.queue, durable=True)
        method, properties, body = channel.basic_get(queue=req.queue, auto_ack=req.auto_ack)

        if method is None:
            conn.close()
            return {"queue": req.queue, "message": None}

        if not req.auto_ack:
            channel.basic_ack(delivery_tag=method.delivery_tag)

        text = body.decode("utf-8", errors="replace") if body else ""
        parsed_payload: Any = text
        if properties and properties.content_type == "application/json" and text:
            try:
                parsed_payload = json.loads(text)
            except json.JSONDecodeError:
                parsed_payload = text

        conn.close()
        return {
            "queue": req.queue,
            "message": {
                "routing_key": method.routing_key,
                "delivery_tag": method.delivery_tag,
                "content_type": properties.content_type if properties else None,
                "payload": parsed_payload,
            },
        }
    except Exception as exc:
        raise HTTPException(status_code=503, detail=f"RabbitMQ consume failed: {exc}") from exc


@app.post("/predict/pediatric-dose", response_model=DoseVerificationResponse)
def predict_pediatric_dose(req: DoseVerificationRequest) -> DoseVerificationResponse:
    return verify_dose(req)


@app.get("/metadata", response_model=MetadataResponse)
def predict_metadata() -> MetadataResponse:
    return renal_metadata()


@app.get("/monitoring", response_model=MonitoringResponse)
def predict_monitoring() -> MonitoringResponse:
    return renal_monitoring()


@app.post("/predict/regression", response_model=RegressionResponse)
def predict_regression(req: PredictRequest) -> RegressionResponse:
    return predict_regression_endpoint(req)


@app.post("/predict/classification", response_model=ClassificationResponse)
def predict_classification(req: PredictRequest, threshold: float = 0.5) -> ClassificationResponse:
    return predict_classification_endpoint(req, threshold)


@app.post("/api/ai/biomarker-extraction/predict", response_model=AIPredictionResponse)
def biomarker_predict_alias(req: AIPredictionRequest) -> AIPredictionResponse:
    return biomarker_predict(req)


@app.post("/api/ai/biomarker-extraction/infer-lab-result", response_model=AiLabAnalysisResponse)
def biomarker_infer_alias(req: AiLabAnalysisRequest) -> AiLabAnalysisResponse:
    return infer_lab_result(req)
