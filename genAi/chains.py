# Custom Langchain chains for GenAI

import asyncio
from langchain.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI
from langchain.chains.mapreduce import MapReduceDocumentsChain
from langchain.chains.llm import LLMChain
from langchain.chains.combine_documents.stuff import StuffDocumentsChain
from response_models import FlashcardResponse, QuizResponse
from langchain_core.documents import Document
from typing import List
from langchain.output_parsers import PydanticOutputParser
from langchain.chains.combine_documents.reduce import ReduceDocumentsChain

class FlashcardChain:
    """
    Custom chain for generating flashcards from a summary.
    """
    def __init__(self, llm):
        llm = llm.with_structured_output(FlashcardResponse)

        self.map_chain = ChatPromptTemplate.from_template(
            """
            Generate 3 flashcards from the following content.

            Each flashcard is an object with:
            - "question"
            - "answer"
            - "difficulty" ("easy", "medium", or "hard")

            Content:
            {doc}
            """
        ) | llm

        self.reduce_chain = ChatPromptTemplate.from_template(
            "Given the below list of flashcards, clean and deduplicate them. Return a final list of flashcards.\n\n"
            "Flashcards:\n{flashcards}\n"
        ) | llm
        
    async def invoke(self, documents: List[Document]):
        """
        Generate flashcards from the provided documents.

        Args:
            documents (list): List of Document objects to process.

        Returns:
            FlashcardResponse: The generated flashcards in structured format.
        """
        contents = [doc.page_content for doc in documents]
        async def process(content: str):
            return (await self.map_chain.ainvoke({"doc": content})).flashcards
        
        # Map in parallel
        results = await asyncio.gather(*[process(c) for c in contents])
        all_flashcards = [fc for group in results for fc in group]

        # Reduce
        joined = "\n".join(str(fc) for fc in all_flashcards)
        reduced = await self.reduce_chain.ainvoke({"flashcards": joined})
        
        return reduced
    
class QuizChain:
    """
    Custom chain for generating a quiz from a summary.
    """
    def __init__(self, llm):
        llm = llm.with_structured_output(QuizResponse)

        self.map_chain = ChatPromptTemplate.from_template(
            """
            Generate a mini (3-4 question) Quiz (MCQ and Short Answer) from the content below.

            Each question should include:
            - type: "mcq" or "short"
            - question
            - correct_answer
            - points (1 point for MCQ, difficulty-based for short answer)
            - options (only for MCQ)

            Content:
            {doc}
            """
        ) | llm

        self.reduce_chain = ChatPromptTemplate.from_template(
            "Given the below list of quiz questions, consolidate, clean, and deduplicate them into a single whole quiz.\n"
            "Ensure a realistic distribution of question types and difficulty levels.\n"
            "Return a quiz with the selected questions.\n\n"
            "Questions:\n{questions}\n"
        ) | llm
        
    async def invoke(self, documents: List[Document]):
        """
        Generate a quiz from the provided documents.

        Args:
            documents (list): List of Document objects to process.

        Returns:
            QuizResponse: The generated quiz in structured format.
        """
        contents = [doc.page_content for doc in documents]
        async def process(content: str):
            mini_quiz: QuizResponse = await self.map_chain.ainvoke({"doc": content})
            return mini_quiz.questions
        
        # Map in parallel
        results = await asyncio.gather(*[process(c) for c in contents])
        all_questions = [q for group in results for q in group]

        # Reduce
        joined = "\n".join(str(q) for q in all_questions)
        reduced: QuizResponse = await self.reduce_chain.ainvoke({"questions": joined})
        
        return reduced 