from contextlib import asynccontextmanager
from fastapi import FastAPI
import os
from rag import rag_cleanup
from request_models import PromptRequest, SummaryRequest, QuizRequest, FlashcardRequest
from llm import StudyLLM



@asynccontextmanager
async def lifespan(_):
    # Startup: init stuff if needed
    yield
    # Shutdown: cleanup
    rag_cleanup()

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
    lifespan=lifespan
)

llm = StudyLLM()

@app.get("/health")
async def health_check():
    """Check the health of the service and its dependencies."""
    try:
        return {"status": "healthy"}
    except Exception as e:
        return {"status": "unhealthy", "error": str(e)}

@app.post("/chat")
async def receive_prompt(data: PromptRequest):
    """
    Receive a prompt and return a response from the LLM.
    """
    response = llm.prompt(data.message)
    return {"response": response}

@app.post("/summary")
async def receive_prompt(data: SummaryRequest):
    """
    Receive a summary reuest and return a summary from the LLM.
    """
    response = llm.summarize(data)
    return {"response": response}

@app.post("/flashcard")
async def receive_prompt(data: FlashcardRequest):
    """
    Receive a flashcard request and return flashcard objects from the LLM.
    """
    return {"message": 'to be implemented'}

@app.post("/quiz")
async def receive_prompt(data: QuizRequest):
    """
    Receive a quiz request and return a quiz object from the LLM.
    """
    return {"message": 'to be implemented'}


# Shut down clean up
@app.on_event("shutdown")
def close_weaviate():
    weaviate_client.close()