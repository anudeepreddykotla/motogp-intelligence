from fastapi import FastAPI

from app.api.health import router as health_router
from app.common.settings import get_settings

settings = get_settings()
app = FastAPI(
    title="MotoGP Intelligence AI Service",
    version="0.1.0",
    description="Internal AI/ML service foundation; future capabilities are intentionally unavailable in Phase 0.",
)
app.include_router(health_router)
