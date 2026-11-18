from fastapi import Header, HTTPException, status

from app.common.settings import get_settings


def require_internal_service_token(authorization: str | None = Header(default=None)) -> None:
    expected = f"Bearer {get_settings().internal_service_token}"
    if authorization != expected:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Valid internal service authentication is required.",
        )
