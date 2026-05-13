FROM python:3.11-slim

WORKDIR /app

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1
ENV PIP_DEFAULT_TIMEOUT=600
ENV PIP_DISABLE_PIP_VERSION_CHECK=1

COPY requirements.txt ./
RUN apt-get update \
    && apt-get install -y --no-install-recommends tesseract-ocr \
    && rm -rf /var/lib/apt/lists/*
RUN pip install --no-cache-dir --retries 10 --timeout 600 -r requirements.txt

COPY app ./app

EXPOSE 8086
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8086"]
