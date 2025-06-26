from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from langchain.chains.summarize import load_summarize_chain
from langchain.prompts import PromptTemplate

from dotenv import load_dotenv
import os

from pydantic import BaseModel

from chains import FlashcardChain
from response_models import FlashcardResponse
from request_models import SummaryRequest

from rag import RAGHelper

load_dotenv()


class StudyLLM:
    llm = ChatOpenAI(
        model="llama3.3:latest",
        temperature=0.5,
        api_key=os.getenv("OPEN_WEBUI_API_KEY"),
        base_url="https://gpu.aet.cit.tum.de/api/"
    )
    
    def __init__(self, doc_path: str):
        base_system_template = ("You are an expert on the information in the context given below.\n"
                                     "Use the context as your primary knowledge source. If you can't fulfill your task given the context, just say that.\n"
                                    "context: {context}\n"
                                    "Your task is {task}"
                                    )
        self.base_prompt_template = ChatPromptTemplate.from_messages([
            ('system', base_system_template),
            ('human', '{input}')
        ])
        try: 
            self.rag_helper = RAGHelper(doc_path)
        except Exception as e:
            raise ValueError(f"Error initializing RAGHelper: {e}")
    
    def _chain(self, output_model: BaseModel = None):
        """
        Construct a chain for the LLM with given configurations.
        
        Args:
            OutputModel (BaseModel, optional): A Pydantic model for structured output.
            ...
        Returns:
            RnnableSequence: The chain for the LLM.
        """
        llm = self.llm
        
        if output_model:
            llm = llm.with_structured_output(output_model)
        
        return self.base_prompt_template | llm

    
    def prompt(self, prompt: str) -> str:
        """
        Call the LLM with a given prompt.
        
        Args:
            prompt (str): The input prompt for the LLM.
        
        Returns:
            str: The response from the LLM.
        """
        task =  (
            "To answer questions based on your context."
            "If you're asked a question that does not relate to your context, do not answer it - instead, answer by saying you're only familiar with <the topic in your context>.\n"
            )
        
        context = self.rag_helper.retrieve(prompt, top_k=5)
        return self._chain().invoke({
            'context': context,
            'task':task,
            'input':prompt
            }).content

    def summarize(self):
        """
        Summarize the given document using the LLM.
        
        Returns:
            str: The summary of the document.
        """
        
        map_prompt = PromptTemplate.from_template(
            (
            f"Write a medium length summary of the following:\n\n"
            "{text}"
            )
        )

        combine_prompt = PromptTemplate.from_template(
            f"""
                Combine the following summaries into one medium length summary **formatted in valid Markdown**.
                Use headings, bullet points, bold/italic text, etc. if appropriate.
                Do not add any preamble or closing sentence.

                Summaries:
                {{text}}
                """
        )

        chain = load_summarize_chain(
            self.llm,
            chain_type="map_reduce",
            map_prompt=map_prompt,
            combine_prompt=combine_prompt
        )

        result = chain.invoke({"input_documents": self.rag_helper.summary_chunks})
        
        return result["output_text"]
        
    async def generate_flashcards(self):
        """
        Generate flashcards from the document using the LLM.
        
        Returns:
            list: A list of flashcard objects.
        """
        flashcard_chain = FlashcardChain(self.llm)
        cards = await flashcard_chain.invoke(self.rag_helper.summary_chunks)
        return cards
    
    def cleanup(self):
        """
        Cleanup resources used by the LLM.
        """
        self.rag_helper.cleanup()