from enum import Enum
from pydantic import BaseModel, Field

# Chat prompt request
class PromptRequest(BaseModel):
    message: str
    
# Summary request
class SummaryLength(str, Enum):
    short = "short"
    medium = "medium"
    long = "long"
    
class SummaryRequest(BaseModel):
    length: SummaryLength


# Flashcard request
class FlashcardRequest(BaseModel):
    count: int = Field(default=10, ge=1, le=35)
    
# Quiz request
class QuizDifficulty(str, Enum):
    easy = "easy"
    medium = "medium"
    hard = "hard"

class QuizRequest(BaseModel):
    question_count: int = Field(default=5, ge=1, le=20)
    difficulty: QuizDifficulty