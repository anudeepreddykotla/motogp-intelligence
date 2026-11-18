from datetime import datetime, timezone

from pydantic import AliasChoices, BaseModel, ConfigDict, Field

from app.common.settings import get_settings


class HealthResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    service: str
    status: str = Field(pattern="^(UP|DOWN)$")
    contract_version: str = Field(
        validation_alias=AliasChoices("contract_version", "contractVersion"),
        serialization_alias="contractVersion",
    )
    checked_at: datetime = Field(
        validation_alias=AliasChoices("checked_at", "checkedAt"),
        serialization_alias="checkedAt",
    )


def healthy_response() -> HealthResponse:
    settings = get_settings()
    return HealthResponse(
        service=settings.service_name,
        status="UP",
        contract_version=settings.contract_version,
        checked_at=datetime.now(timezone.utc),
    )
