import argparse
import sys
from pathlib import Path

SERVICE_ROOT = Path(__file__).resolve().parents[1]
if str(SERVICE_ROOT) not in sys.path:
    sys.path.insert(0, str(SERVICE_ROOT))

import joblib


def main() -> None:
    parser = argparse.ArgumentParser(description="Verify exported AI triage artifact.")
    parser.add_argument(
        "--model",
        default="BackEnd/microservices/ai-triage-service/models/followup_triage.joblib",
        help="Path to the exported joblib artifact.",
    )
    args = parser.parse_args()

    model_path = Path(args.model)
    model = joblib.load(model_path)
    sample = {
        "message_text": "Bonjour, fievre a 39 et douleurs depuis hier soir",
        "subject": "Symptomes",
        "message_type": "MEDICAL",
        "guardian_priority": "HIGH",
    }
    prediction = model.predict([sample])[0]
    confidence = max(model.predict_proba([sample])[0])
    print(f"Prediction: {prediction}")
    print(f"Confidence: {confidence:.4f}")


if __name__ == "__main__":
    main()

