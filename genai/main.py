import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.responses import JSONResponse
from helpers import save_document
from request_models import LoadDocumentRequest, PromptRequest, SummaryRequest, QuizRequest, FlashcardRequest
from llm import StudyLLM
from prometheus_fastapi_instrumentator import Instrumentator

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


llm_instance: StudyLLM = StudyLLM()

@asynccontextmanager
async def lifespan(_):
    yield
    # Shutdown: cleanup
    llm_instance.cleanup()

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

Instrumentator(
    excluded_handlers=['/metrics'],
    should_group_status_codes=False,
    should_instrument_requests_inprogress=True
    ).instrument(app).expose(app)


# StudyLLM(user_id='dummy', doc_path="./documents/example/dummy_knowledge.txt") # TODO: remove
# llm_instances["dummy2"] = StudyLLM(user_id='dummy', doc_path="./documents/example/W07_Microservices_and_Scalable_Architectures.pdf") # TODO: remove

# Auxiliary Endpoints
@app.get("/health")
async def health_check():
    """Check the health of the service and its dependencies."""
    try:
        return {"status": "healthy"}
    except Exception as e:
        return {"status": "unhealthy", "error": str(e)}


# AI Tasks Endpoints
@app.post("/document")
async def load_document(data: LoadDocumentRequest):
    """
    Load a new document in the LLM instance.
    """
    try:
        logger.info(f"Loading new document {data.document_name} for user {data.user_id}")
        doc_name = f"{data.user_id}_{data.document_name}"
        path = save_document(doc_name, data.document_base64)
        await llm_instance.load_document(doc_name, path, data.user_id)
        logger.info(f"Document {doc_name} created successfully")
        return {"message": "Document loaded successfully."}
    except Exception as e:
        error_msg = f"Failed to load document {doc_name}: {str(e)}"
        logger.error(error_msg)
        return {"error": error_msg}

@app.post("/chat")
async def receive_prompt(data: PromptRequest):
    """
    Receive a prompt and return a response from the LLM.
    """
    try:
        logger.info(f"Processing chat request for user {data.user_id}")
        response = await llm_instance.prompt(data.message, data.user_id)
        return {"response": response}
    except Exception as e:
        error_msg = f"Chat error for user {data.user_id}: {str(e)}"
        logger.error(error_msg)
        return {"response": f"ERROR: {error_msg}"}

@app.post("/summary")
async def generate_summary(data: SummaryRequest):
    """
    Receive a summary request and return a summary from the LLM.
    """
    try:
        logger.info(f"Generating summary for user {data.user_id}, document {data.document_name}")
        response = await llm_instance.summarize(data.document_name, data.user_id)
        return {"response": response}
    except Exception as e:
        error_msg = f"Summary generation error for user {data.user_id}: {str(e)}"
        logger.error(error_msg)
        return {"response": f"ERROR: {error_msg}"}

@app.post("/flashcard")
async def generate_flashcards(data: FlashcardRequest):
    """
    Receive a flashcard request and return flashcard objects from the LLM.
    """
    try:
        logger.info(f"Generating flashcards for user {data.user_id}, document {data.document_name}")
        response = await llm_instance.generate_flashcards(data.document_name, data.user_id)
        logger.info(f"Flashcards generated successfully for user {data.user_id}")
        return {"response": response}
    except Exception as e:
        error_msg = f"Flashcard generation error for user {data.user_id}: {str(e)}"
        logger.error(error_msg)
        return {"response": {"flashcards": [], "error": error_msg}}

@app.post("/quiz")
async def generate_quiz(data: QuizRequest):
    """
    Receive a quiz request and return a quiz object from the LLM.
    """
    try:
        logger.info(f"Generating quiz for user {data.user_id}, document {data.document_name}")
        response = await llm_instance.generate_quiz(data.document_name, data.user_id)
        logger.info(f"Quiz generated successfully for user {data.user_id}")
        return {"response": response}
    except Exception as e:
        error_msg = f"Quiz generation error for user {data.user_id}: {str(e)}"
        logger.error(error_msg)
        return {"response": {"questions": [], "error": error_msg}}
