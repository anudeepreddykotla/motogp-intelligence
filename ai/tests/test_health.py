from fastapi.testclient import TestClient

from app.main import app
from app.common.settings import get_settings


client = TestClient(app)


def test_public_health_is_available() -> None:
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "UP"


def test_internal_health_requires_authentication() -> None:
    response = client.get("/internal/v1/health")
    assert response.status_code == 401


def test_internal_health_accepts_service_token() -> None:
    token = get_settings().internal_service_token
    response = client.get("/internal/v1/health", headers={"Authorization": f"Bearer {token}"})
    assert response.status_code == 200
    assert response.json()["contractVersion"] == "1.0"
