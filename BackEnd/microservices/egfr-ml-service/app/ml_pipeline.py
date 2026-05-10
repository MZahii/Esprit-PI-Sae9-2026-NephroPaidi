from __future__ import annotations

import numpy as np
import pandas as pd


def bool_to_int(x):
    """
    Compatibility helper for legacy pickled sklearn pipelines.
    Converts boolean-like features to 0/1 integers.
    """
    if isinstance(x, pd.DataFrame):
        return x.apply(lambda col: col.astype(bool).astype(int))
    if isinstance(x, pd.Series):
        return x.astype(bool).astype(int)
    arr = np.asarray(x)
    return arr.astype(bool).astype(int)

