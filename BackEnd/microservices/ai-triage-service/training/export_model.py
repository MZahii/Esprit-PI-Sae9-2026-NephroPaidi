import argparse
import json
import sys
from datetime import datetime, timezone
from pathlib import Path

SERVICE_ROOT = Path(__file__).resolve().parents[1]
if str(SERVICE_ROOT) not in sys.path:
    sys.path.insert(0, str(SERVICE_ROOT))

import joblib
import pandas as pd
import sklearn
from sklearn.compose import ColumnTransformer
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, classification_report, f1_score
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler

from app.pipeline import CreationTimeFeatureBuilder


LABEL_ORDER = ["LOW", "MEDIUM", "HIGH", "CRITICAL"]
SAFE_FEATURE_CONTRACT = [
    "message_text",
    "subject",
    "message_type",
    "guardian_priority",
]


def main() -> None:
    parser = argparse.ArgumentParser(description="Export the follow-up message AI triage model.")
    parser.add_argument(
        "--input",
        default=r"C:\Users\hp\Downloads\data_modeling\FollowUp_Messages_Cleaned.csv",
        help="Path to FollowUp_Messages_Cleaned.csv.",
    )
    parser.add_argument(
        "--output-dir",
        default="BackEnd/microservices/ai-triage-service/models",
        help="Directory where model and metadata will be written.",
    )
    parser.add_argument("--version", default="followup-triage-safe-v1")
    args = parser.parse_args()

    input_path = Path(args.input)
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    dataset = load_safe_training_data(input_path)
    X = dataset[SAFE_FEATURE_CONTRACT]
    y = dataset["urgency"]

    X_train, X_test, y_train, y_test = train_test_split(
        X,
        y,
        test_size=0.2,
        random_state=42,
        stratify=y,
    )

    model = build_pipeline()
    model.fit(X_train.to_dict(orient="records"), y_train)

    predictions = model.predict(X_test.to_dict(orient="records"))
    report = classification_report(y_test, predictions, labels=LABEL_ORDER, output_dict=True, zero_division=0)

    model_path = output_dir / "followup_triage.joblib"
    metadata_path = output_dir / "followup_triage_metadata.json"
    joblib.dump(model, model_path)

    notes = [
        "This artifact intentionally avoids future workflow fields.",
        "The available dataset lacks the application's exact MessageType field; training uses OTHER.",
        "The available dataset lacks guardian-submitted priority; training uses UNKNOWN.",
    ]
    accuracy = accuracy_score(y_test, predictions)
    weighted_f1 = f1_score(y_test, predictions, average="weighted", zero_division=0)
    macro_f1 = f1_score(y_test, predictions, average="macro", zero_division=0)
    if accuracy >= 0.99:
        notes.append(
            "Very high test metrics should be treated cautiously; the dataset may be synthetic, templated, or highly separable."
        )

    metadata = {
        "modelVersion": args.version,
        "trainedAt": datetime.now(timezone.utc).isoformat(),
        "sklearnVersion": sklearn.__version__,
        "sourceDataset": str(input_path),
        "trainingRows": int(len(X_train)),
        "testRows": int(len(X_test)),
        "safeFeatureContract": SAFE_FEATURE_CONTRACT,
        "excludedLeakageFamilies": [
            "assigned role",
            "queue",
            "status",
            "escalation outcome",
            "response time",
            "read state",
            "staff actions",
            "previous AI intent/sentiment/confidence outputs",
        ],
        "labels": LABEL_ORDER,
        "metrics": {
            "accuracy": accuracy,
            "weightedF1": weighted_f1,
            "macroF1": macro_f1,
            "classificationReport": report,
        },
        "notes": notes,
    }
    metadata_path.write_text(json.dumps(metadata, indent=2), encoding="utf-8")

    print(f"Model written to: {model_path}")
    print(f"Metadata written to: {metadata_path}")
    print(f"Accuracy: {metadata['metrics']['accuracy']:.4f}")
    print(f"Weighted F1: {metadata['metrics']['weightedF1']:.4f}")
    print(f"Macro F1: {metadata['metrics']['macroF1']:.4f}")


def load_safe_training_data(input_path: Path) -> pd.DataFrame:
    df = pd.read_csv(input_path)
    required_columns = {"Message_Text", "AI_Urgency_Level"}
    missing = required_columns - set(df.columns)
    if missing:
        raise ValueError(f"Missing required columns: {sorted(missing)}")

    safe = pd.DataFrame(
        {
            "message_text": df["Message_Text"].fillna("").astype(str),
            "subject": "",
            "message_type": "OTHER",
            "guardian_priority": "UNKNOWN",
            "urgency": df["AI_Urgency_Level"].fillna("").astype(str).str.upper().str.strip(),
        }
    )
    safe = safe[safe["message_text"].str.strip().ne("")]
    safe = safe[safe["urgency"].isin(LABEL_ORDER)]
    if safe.empty:
        raise ValueError("No usable training rows after safe filtering.")
    return safe


def build_pipeline() -> Pipeline:
    feature_preprocessor = ColumnTransformer(
        transformers=[
            ("text", TfidfVectorizer(ngram_range=(1, 2), min_df=2, max_features=4000), "combined_text"),
            ("categorical", OneHotEncoder(handle_unknown="ignore"), ["message_type", "guardian_priority"]),
            (
                "numeric",
                StandardScaler(),
                [
                    "character_count",
                    "word_count",
                    "has_medical_keyword",
                    "has_urgent_keyword",
                    "has_critical_keyword",
                    "question_mark_count",
                    "exclamation_count",
                ],
            ),
        ]
    )

    classifier = LogisticRegression(
        max_iter=1000,
        class_weight="balanced",
        random_state=42,
    )

    return Pipeline(
        steps=[
            ("features", CreationTimeFeatureBuilder()),
            ("preprocess", feature_preprocessor),
            ("classifier", classifier),
        ]
    )


if __name__ == "__main__":
    main()

