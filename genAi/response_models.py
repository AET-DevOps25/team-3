from typing import Literal, Union
from pydantic import BaseModel, Field


# Flashcards
class FlashcardModel(BaseModel):
    question: str = Field(
        ...,
        description="The question to be answered"
    )
    answer: str = Field(
        ...,
        description="The answer to the question"
    )
    difficulty: str = Field(
        ...,
        description="The difficulty level of the flashcard (e.g., easy, medium, hard)"
    )   

class FlashcardResponse(BaseModel):
    flashcards: list[FlashcardModel] = Field(
        ...,
        description="A list of flashcards generated by the LLM"
    )

# Quizzes
class QuestionModel(BaseModel):
    type: str # discriminator field
    question: str = Field(
        ...,
        description="The question to be answered"
    )
    correct_answer: str = Field(
        ...,
        description="The correct answer to the question"
    )
    points: int = Field(
        ...,
        description="The points awarded for answering the question correctly"
    )

class MCQQuestionModel(QuestionModel):
    type: Literal["mcq"]
    options: list[str] = Field(
        ...,
        description="A list of options for the multiple-choice question"
    )
    
class ShortAnswerQuestionModel(QuestionModel):
    type: Literal["short"]

class QuizResponse(BaseModel):
    questions: list[Union[MCQQuestionModel, ShortAnswerQuestionModel]] = Field(
        ...,
        description="A list of questions in the quiz"
    )
    # total_points: int = Field(
    #     ...,
    #     description="The total points available in the quiz"
    # )