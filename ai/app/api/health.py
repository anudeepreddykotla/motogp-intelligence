from fastapi import APIRouter, Depends

from app.common.auth import require_internal_service_token
from app.common.contracts import HealthResponse, healthy_response

router = APIRouter()


@router.get("/health", response_model=HealthResponse, tags=["health"])
def public_health() -> HealthResponse:
    return healthy_response()


@router.get(
    "/internal/v1/health",
    response_model=HealthResponse,
    dependencies=[Depends(require_internal_service_token)],
    tags=["internal-health"],
)
def internal_health() -> HealthResponse:
    return healthy_response()
