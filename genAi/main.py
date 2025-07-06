import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from helpers import save_document
from request_models import CreateSessionRequest, PromptRequest, SummaryRequest, QuizRequest, FlashcardRequest
from llm import StudyLLM

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


llm_instances: dict[str, StudyLLM] = {}

@asynccontextmanager
async def lifespan(_):
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

# llm_instances["dummy"] = StudyLLM("./documents/example/W07_Microservices_and_Scalable_Architectures.pdf") # TODO: remove
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
    try:
        if data.session_id in llm_instances:
            logger.info(f"Session {data.session_id} already exists")
            return {"message": "Session already loaded."}
        
        logger.info(f"Creating new session {data.session_id} for document {data.document_name}")
        doc_name = f"{data.session_id}_{data.document_name}"
        path = save_document(doc_name, data.document_base64)
        llm_instances[data.session_id] = StudyLLM(path)
        logger.info(f"Session {data.session_id} created successfully")
        return {"message": "Session created successfully."}
    except Exception as e:
        error_msg = f"Failed to create session {data.session_id}: {str(e)}"
        logger.error(error_msg)
        return {"error": error_msg}


@app.post("/chat")
async def receive_prompt(data: PromptRequest):
    """
    Receive a prompt and return a response from the LLM.
    """
    try:
        if data.session_id not in llm_instances:
            error_msg = f"Session {data.session_id} not found. Please ensure the document was processed successfully."
            logger.error(error_msg)
            return {"response": f"ERROR: {error_msg}"}
        
        logger.info(f"Processing chat request for session {data.session_id}")
        response = llm_instances[data.session_id].prompt(data.message)
        return {"response": response}
    except Exception as e:
        error_msg = f"Chat error for session {data.session_id}: {str(e)}"
        logger.error(error_msg)
        return {"response": f"ERROR: {error_msg}"}

@app.post("/summary")
async def generate_summary(data: SummaryRequest):
    """
    Receive a summary request and return a summary from the LLM.
    """
    try:
        if data.session_id not in llm_instances:
            error_msg = f"Session {data.session_id} not found. Please ensure the document was processed successfully."
            logger.error(error_msg)
            return {"response": f"ERROR: {error_msg}"}
        
        logger.info(f"Generating summary for session {data.session_id}")
        response = llm_instances[data.session_id].summarize()
        logger.info(f"Summary generated successfully for session {data.session_id}")
        return {"response": response}
    except Exception as e:
        error_msg = f"Summary generation error for session {data.session_id}: {str(e)}"
        logger.error(error_msg)
        return {"response": f"ERROR: {error_msg}"}

@app.post("/flashcard")
async def generate_flashcards(data: FlashcardRequest):
    """
    Receive a flashcard request and return flashcard objects from the LLM.
    """
    try:
        if data.session_id not in llm_instances:
            error_msg = f"Session {data.session_id} not found. Please ensure the document was processed successfully."
            logger.error(error_msg)
            return {"response": {"flashcards": [], "error": error_msg}}
        
        logger.info(f"Generating flashcards for session {data.session_id}")
        response = await llm_instances[data.session_id].generate_flashcards()
        logger.info(f"Flashcards generated successfully for session {data.session_id}")
        return {"response": response}
    except Exception as e:
        error_msg = f"Flashcard generation error for session {data.session_id}: {str(e)}"
        logger.error(error_msg)
        return {"response": {"flashcards": [], "error": error_msg}}

@app.post("/quiz")
async def generate_quiz(data: QuizRequest):
    """
    Receive a quiz request and return a quiz object from the LLM.
    """
    try:
        if data.session_id not in llm_instances:
            error_msg = f"Session {data.session_id} not found. Please ensure the document was processed successfully."
            logger.error(error_msg)
            return {"response": {"questions": [], "error": error_msg}}
        
        logger.info(f"Generating quiz for session {data.session_id}")
        response = await llm_instances[data.session_id].generate_quiz()
        logger.info(f"Quiz generated successfully for session {data.session_id}")
        return {"response": response}
    except Exception as e:
        error_msg = f"Quiz generation error for session {data.session_id}: {str(e)}"
        logger.error(error_msg)
        return {"response": {"questions": [], "error": error_msg}}