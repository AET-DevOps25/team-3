from contextlib import asynccontextmanager
from fastapi import FastAPI
from helpers import save_document
from request_models import CreateSessionRequest, PromptRequest, SummaryRequest, QuizRequest, FlashcardRequest
from llm import StudyLLM


llm_instances: dict[str, StudyLLM] = {}

@asynccontextmanager
async def lifespan(_):
    # Startup: init stuff if needed
    yield
    # Shutdown: cleanup
    for llm in llm_instances.values():
        llm.cleanup()

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

llm_instances["dummy"] = StudyLLM("./documents/example/W07_Microservices_and_Scalable_Architectures.pdf") # TODO: remove
# llm_instances["dummy2"] = StudyLLM("./documents/example/dummy_knowledge.txt") # TODO: remove

@app.get("/health")
async def health_check():
    """Check the health of the service and its dependencies."""
    try:
        return {"status": "healthy"}
    except Exception as e:
        return {"status": "unhealthy", "error": str(e)}


@app.post("/session/load")
async def load_session(data: CreateSessionRequest):
    """
    Create a new session with the LLM for a given document URL.
    """
    if data.session_id in llm_instances:
        return {"message": "Session already loaded."}
    
    try:
        doc_name = f"{data.session_id}_{data.document_name}"
        path = save_document(doc_name, data.document_base64)
        llm_instances[data.session_id] = StudyLLM(path)
        return {"message": "Session created successfully."}
    except Exception as e:
        return {"error": str(e)}


@app.post("/chat")
async def receive_prompt(data: PromptRequest):
    """
    Receive a prompt and return a response from the LLM.
    """
    response = llm_instances[data.session_id].prompt(data.message)
    return {"response": response}

@app.post("/summary")
async def receive_prompt(data: SummaryRequest):
    """
    Receive a summary reuest and return a summary from the LLM.
    """
    response = llm_instances[data.session_id].summarize()
    return {"response": response}

@app.post("/flashcard")
async def receive_prompt(data: FlashcardRequest):
    """
    Receive a flashcard request and return flashcard objects from the LLM.
    """
    response = await llm_instances[data.session_id].generate_flashcards()
    return {"response": response}

@app.post("/quiz")
async def receive_prompt(data: QuizRequest):
    """
    Receive a quiz request and return a quiz object from the LLM.
    """
    response = await llm_instances[data.session_id].generate_quiz()
    return {"response": response}