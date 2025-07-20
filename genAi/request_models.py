from enum import Enum
from pydantic import BaseModel

class BaseLLMRequest(BaseModel):
    """
    Base class for all LLM requests.
    """
    session_id: str

# Create session request
class CreateSessionRequest(BaseLLMRequest):
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
    # length: SummaryLength
    pass


# Flashcard request
class FlashcardRequest(BaseLLMRequest):
    # count: int = Field(default=10, ge=1, le=35)
    pass
    
# Quiz request
class QuizDifficulty(str, Enum):
    easy = "easy"
    medium = "medium"
    hard = "hard"

class QuizRequest(BaseLLMRequest):
    # question_count: int = Field(default=5, ge=1, le=20)
    # difficulty: QuizDifficulty
    pass