from __future__ import annotations

from collections import deque
from dataclasses import dataclass, field
from typing import Any, Deque, Dict


@dataclass
class MonitoringState:
    requests_total: int = 0
    validation_errors: int = 0
    inference_errors: int = 0
    low_confidence_predictions: int = 0
    recent_events: Deque[Dict[str, Any]] = field(default_factory=lambda: deque(maxlen=50))

    def record_event(self, event: Dict[str, Any]) -> None:
        self.recent_events.appendleft(event)

    def snapshot(self) -> Dict[str, Any]:
        return {
            "requests_total": self.requests_total,
            "validation_errors": self.validation_errors,
            "inference_errors": self.inference_errors,
            "low_confidence_predictions": self.low_confidence_predictions,
            "recent_events": list(self.recent_events),
        }


monitoring_state = MonitoringState()

