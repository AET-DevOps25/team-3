from pydantic import BaseModel, Field

class SummaryResponse(BaseModel):
    summary: str = Field(
        ...,
        description="The generated summary text"
    )