from enum import Enum
from pydantic import BaseModel

class BaseLLMRequest(BaseModel):
    """
    Base class for all LLM requests.
    """
    user_id: str

# Load document request
class LoadDocumentRequest(BaseLLMRequest):
    document_name: str
    document_base64: str

# Chat prompt request
class PromptRequest(BaseLLMRequest):
    message: str
    
# Summary request
class SummaryLength(str, Enum):
    short = "short"
    medium = "medium"
    long = "long"
    
class SummaryRequest(BaseLLMRequest):
    document_name: str


# Flashcard request
class FlashcardRequest(BaseLLMRequest):
    document_name: str
    
# Quiz request
class QuizDifficulty(str, Enum):
    easy = "easy"
    medium = "medium"
    hard = "hard"

class QuizRequest(BaseLLMRequest):
    document_name: str