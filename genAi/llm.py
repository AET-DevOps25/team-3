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
    # Class-level attributes for lazy initialization
    _chat_llm = None
    _generation_llm = None

    @classmethod
    def _get_chat_llm(cls):
        """Lazy initialization of chat LLM"""
        if cls._chat_llm is None:
            cls._chat_llm = ChatOpenAI(
                model="llama3.3:latest",
                temperature=0.5,
                api_key=os.getenv("OPEN_WEBUI_API_KEY_CHAT"),
                base_url="https://gpu.aet.cit.tum.de/api/",
            )
        return cls._chat_llm

    @classmethod
    def _get_generation_llm(cls):
        """Lazy initialization of generation LLM"""
        if cls._generation_llm is None:
            cls._generation_llm = ChatOpenAI(
                model="llama3.3:latest",
                temperature=0.5,
                api_key=os.getenv("OPEN_WEBUI_API_KEY_GEN"),
                base_url="https://gpu.aet.cit.tum.de/api/",
            )
        return cls._generation_llm

    @property
    def chat_llm(self):
        """Get the chat LLM instance"""
        return self._get_chat_llm()

    @chat_llm.setter
    def chat_llm(self, value):
        """Set the chat LLM instance (for testing)"""
        StudyLLM._chat_llm = value

    @chat_llm.deleter
    def chat_llm(self):
        """Reset the chat LLM instance (for testing)"""
        StudyLLM._chat_llm = None

    @property
    def generation_llm(self):
        """Get the generation LLM instance"""
        return self._get_generation_llm()

    @generation_llm.setter
    def generation_llm(self, value):
        """Set the generation LLM instance (for testing)"""
        StudyLLM._generation_llm = value

    @generation_llm.deleter
    def generation_llm(self):
        """Reset the generation LLM instance (for testing)"""
        StudyLLM._generation_llm = None

    def __init__(self, doc_path: str):
        base_system_template = (
            "You are an expert on the information in the context given below.\n"
            "Use the context as your primary knowledge source. If you can't fulfill your task given the context, just say that.\n"
            "context: {context}\n"
            "Your task is {task}"
        )
        self.base_prompt_template = ChatPromptTemplate.from_messages(
            [("system", base_system_template), ("human", "{input}")]
        )
        try:
            self.rag_helper = RAGHelper(doc_path)
        except Exception as e:
            raise ValueError(f"Error initializing RAGHelper: {e}")

    async def prompt(self, prompt: str) -> str:
        """
        Call the LLM with a given prompt.

        Args:
            prompt (str): The input prompt for the LLM.

        Returns:
            str: The response from the LLM.
        """
        task = (
            "To answer questions based on your context."
            "If you're asked a question that does not relate to your context, do not answer it - instead, answer by saying you're only familiar with <the topic in your context>.\n"
        )

        context = self.rag_helper.retrieve(prompt, top_k=5)
        chain = self.base_prompt_template | self.chat_llm
        response = await chain.ainvoke(
            {"context": context, "task": task, "input": prompt}
        )

        return response.content

    async def summarize(self):
        """
        Summarize the given document using the LLM.

        Returns:
            str: The summary of the document.
        """

        map_prompt = PromptTemplate.from_template(
            (f"Write a medium length summary of the following:\n\n" "{text}")
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
            combine_prompt=combine_prompt,
        )

        result = await chain.ainvoke(
            {"input_documents": self.rag_helper.summary_chunks}
        )

        return result["output_text"]

    async def generate_flashcards(self):
        """
        Generate flashcards from the document using the LLM.

        Returns:
            list: A list of flashcard objects.
        """
        flashcard_chain = FlashcardChain(self.generation_llm)
        cards = await flashcard_chain.invoke(self.rag_helper.summary_chunks)
        return cards

    async def generate_quiz(self):
        """
        Generate a quiz from the document using the LLM.

        Returns:
            list: A quiz object.
        """
        quiz_chain = QuizChain(self.generation_llm)
        quiz = await quiz_chain.invoke(self.rag_helper.summary_chunks)
        return quiz

    def cleanup(self):
        """
        Cleanup resources used by the LLM.
        """
        try:
            self.rag_helper.cleanup()
        except Exception as e:
            print(f"Error during RAGHelper cleanup: {e}")
