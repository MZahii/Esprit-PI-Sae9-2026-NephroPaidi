from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent
MODEL_DIR = BASE_DIR / "model"

REG_MODEL_PATH = MODEL_DIR / "regression_model.pkl"
REG_MODEL_3M_PATH = MODEL_DIR / "regression_model_3m.pkl"
REG_MODEL_6M_PATH = MODEL_DIR / "regression_model_6m.pkl"
REG_MODEL_12M_PATH = MODEL_DIR / "regression_model_12m.pkl"
CLS_MODEL_PATH = MODEL_DIR / "classification_model.pkl"
FEATURES_PATH = MODEL_DIR / "feature_columns.json"
FEATURE_SCHEMA_PATH = MODEL_DIR / "feature_schema.json"
