from langchain_openai import ChatOpenAI
from dotenv import load_dotenv
import os

load_dotenv()

class StudyLLM:
        
    llm = ChatOpenAI(
        model="llama3.3:latest",
        temperature=0.5,
        api_key=os.getenv("OPEN_WEBUI_API_KEY"),
        base_url="https://gpu.aet.cit.tum.de/api/"
    )
    
    chain = llm
    
    def __init__(self):
        pass
    

    
    def call(self, prompt: str) -> str:
        """
        Call the LLM with a given prompt.
        
        Args:
            prompt (str): The input prompt for the LLM.
        
        Returns:
            str: The response from the LLM.
        """
        return self.chain.invoke(prompt)