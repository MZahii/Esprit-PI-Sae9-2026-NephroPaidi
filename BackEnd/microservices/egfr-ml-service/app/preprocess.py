from __future__ import annotations

from typing import Dict, List

import numpy as np
import pandas as pd


def align_payload_to_features(payload: dict, feature_columns: List[str], schema: Dict[str, List[str]]) -> pd.DataFrame:
    row = {col: payload.get(col, np.nan) for col in feature_columns}

    for col in schema.get("bool_cols", []):
        if col not in payload:
            row[col] = False

    x = pd.DataFrame([row])

    for col in schema.get("num_cols", []):
        if col in x.columns:
            x[col] = pd.to_numeric(x[col], errors="coerce")

    for col in schema.get("bool_cols", []):
        if col in x.columns:
            x[col] = x[col].fillna(False).astype(bool)

    return x
