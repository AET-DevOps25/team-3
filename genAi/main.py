from fastapi import FastAPI
import os
import weaviate
from langchain_groq import ChatGroq
from llm import StudyLLM
from pydantic import BaseModel

# Initialize Weaviate client
weaviate_url = "http://localhost:8082" # Replace with url to weavite container from .env
client = weaviate.Client(url=weaviate_url)

class PromptRequest(BaseModel):
    prompt: str


app = FastAPI(
    title="tutor",
    openapi_tags=[
        {
            "name": "Health",
            "description": "Endpoints to check the health of the service.",
        },
        {
            "name": "Pool",
            "description": "Endpoints to manage data collections.",
        },
        {
            "name": "Search",
            "description": "Endpoints for searching within data collections.",
        },
        {"name": "Ingestion", "description": "Endpoints to start ingestion processes."},
    ],
)

llm = StudyLLM()

@app.get("/health")
async def health_check():
    """Check the health of the service and its dependencies."""
    try:
        # Check if Weaviate is accessible
        client.is_ready()
        return {"status": "healthy", "weaviate": "connected"}
    except Exception as e:
        return {"status": "unhealthy", "error": str(e)}

@app.post("/prompt")
async def receive_prompt(data: PromptRequest):
    """
    Receive a prompt and return a response from the LLM.
    """
    response = llm.call(data.prompt)
    return {"response": response}