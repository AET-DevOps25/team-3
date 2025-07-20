from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from langchain.chains.summarize import load_summarize_chain
from langchain.prompts import PromptTemplate

from dotenv import load_dotenv
import os
from chains import FlashcardChain, QuizChain
from rag import RAGHelper

load_dotenv()


class StudyLLM:
    # for chat
    chat_llm = ChatOpenAI(
        model="llama3.3:latest",
        temperature=0.5,
        api_key=os.getenv("OPEN_WEBUI_API_KEY_CHAT"),
        base_url="https://gpu.aet.cit.tum.de/api/"
    )
    
    # For summaries, quizzes, flashcards
    generation_llm = ChatOpenAI(
        model="llama3.3:latest",
        temperature=0.5,
        api_key=os.getenv("OPEN_WEBUI_API_KEY_GEN"),
        base_url="https://gpu.aet.cit.tum.de/api/"
    )
    
    def __init__(self):
        base_system_template = ("You are an expert on the information in the context given below.\n"
                                     "Use the context as your primary knowledge source.\n"
                                    "Do not mention that you have a context, just use it to fulfill your task.\n"
                                    "context: {context}\n"
                                    "Your task is {task}"
                                    )
        self.base_prompt_template = ChatPromptTemplate.from_messages([
            ('system', base_system_template),
            ('human', '{input}')
        ])
        try: 
            self.rag_helper = RAGHelper()
        except Exception as e:
            raise ValueError(f"Error initializing RAGHelper: {e}")

    
    async def load_document(self, doc_name: str, path: str, user_id: str):
        """
        Load a document into the RAG system.
        
        Args:
            doc_name (str): The name of the document to load.
            user_id (str): The ID of the user loading the document.
        """
        try:
            await self.rag_helper.load_document(doc_name, path, user_id)
            return f"Document {doc_name} loaded successfully for user {user_id}."
        except Exception as e:
            error_msg = f"Failed to load document {doc_name} for user {user_id}: {str(e)}"
            raise ValueError(error_msg)
    
    async def prompt(self, prompt: str, user_id: str) -> str:
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
        
        context = self.rag_helper.retrieve(query=prompt, user_id=user_id, top_k=5)
        chain = self.base_prompt_template | self.chat_llm
        response = await chain.ainvoke({
            'context': context,
            'task':task,
            'input':prompt
            })
        
        return response.content

    async def summarize(self, document_name: str, user_id: str) -> str:
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
            self.generation_llm,
            chain_type="map_reduce",
            map_prompt=map_prompt,
            combine_prompt=combine_prompt
        )

        doc_name = f"{user_id}_{document_name}"
        chunks = self.rag_helper.get_generation_chunks(user_id, doc_name)
        result = await chain.ainvoke({"input_documents": chunks})
        
        return result["output_text"]
        
    async def generate_flashcards(self, document_name: str, user_id: str):
        """
        Generate flashcards from the document using the LLM.
        
        Returns:
            list: A list of flashcard objects.
        """
        flashcard_chain = FlashcardChain(self.generation_llm)
        doc_name = f"{user_id}_{document_name}"
        chunks = self.rag_helper.get_generation_chunks(user_id, doc_name)
        cards = await flashcard_chain.invoke(chunks)
        return cards
    
    async def generate_quiz(self, document_name: str, user_id: str):
        """
        Generate a quiz from the document using the LLM.
        
        Returns:
            list: A quiz object.
        """
        quiz_chain = QuizChain(self.generation_llm)
        doc_name = f"{user_id}_{document_name}"
        chunks = self.rag_helper.get_generation_chunks(user_id, doc_name)
        quiz = await quiz_chain.invoke(chunks)
        return quiz
    
    def cleanup(self):
        """
        Cleanup resources used by the LLM.
        """
        self.rag_helper.cleanup()