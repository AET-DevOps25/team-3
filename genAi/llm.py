from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate

from dotenv import load_dotenv
import os

from pydantic import BaseModel

from request_models import SummaryRequest
from response_models import SummaryResponse

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

    def summarize(self, request: SummaryRequest):
        """
        Summarize the given document using the LLM.
        
        Args:
            request (SummaryRequest): The request containing summary preferences.
        
        Returns:
            str: The summary of the document.
        """
        task = "to summarize the text in your knowledge."
        return self._chain(output_model=SummaryResponse).invoke({
            'task': task,
            'input': f"summary length: {request.length.value}"
        })
        
    
    def cleanup(self):
        """
        Cleanup resources used by the LLM.
        """
        self.rag_helper.cleanup()