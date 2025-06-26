# Custom Langchain chains for GenAI

import asyncio
from langchain.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI
from langchain.chains.mapreduce import MapReduceDocumentsChain
from langchain.chains.llm import LLMChain
from langchain.chains.combine_documents.stuff import StuffDocumentsChain
from response_models import FlashcardResponse
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