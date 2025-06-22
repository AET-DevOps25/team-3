import weaviate
from langchain_community.document_loaders import PyMuPDFLoader, TextLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_weaviate.vectorstores import WeaviateVectorStore
from langchain_cohere import CohereEmbeddings
from langchain_core.documents import Document
from dotenv import load_dotenv
import os


def _get_loader(doc_path: str):
    """
    Validates path and returns a document loader for the given file type.
    Args:
        doc_path (str): The path to the document file.
    Returns:
        PyMuPDFLoader or TextLoader: A document loader for the specified file type.
    """
    if not os.path.exists(doc_path):
        raise FileNotFoundError(f"The document path {doc_path} does not exist.")
    if doc_path.endswith('.pdf'):
        print(f"Loading PDF file: {doc_path}")
        return PyMuPDFLoader(doc_path)
    elif doc_path.endswith('.txt'):
        print(f"Loading text file: {doc_path}")
        return TextLoader(doc_path)
    else:
        raise ValueError("Unsupported file type. Please provide a .pdf or .txt file.")

class RAGHelper:
    """
    A helper for the retrieval stage of the RAG pipeline.
    """
    
    def __init__(self, doc_path: str):
        # Load documents
        try:
            loader = _get_loader(doc_path)
        except Exception as e:
            raise ValueError(f"Error loading document: {e}")
        
        documents = loader.load()
        # Split document into smaller chunks
        retrieval_splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=200)
        split_documents = retrieval_splitter.split_documents(documents)
        
        # Initialize embeddings model
        load_dotenv()
        embeddings_model = CohereEmbeddings(model="embed-english-light-v3.0", cohere_api_key=os.getenv("COHERE_API_KEY"))

        # Initialize Weaviate client
        self.weaviate_client = weaviate.connect_to_local()
        self.db = WeaviateVectorStore.from_documents(split_documents, embeddings_model, client=self.weaviate_client)
        # Split documents for summarization, flashcards, and quiz generation.
        ## combine all documents into a single text (avoid 1 document per page)
        full_text = "\n\n".join([doc.page_content for doc in documents])
        combined_doc = Document(page_content=full_text)
        summary_splitter = RecursiveCharacterTextSplitter(chunk_size=4000, chunk_overlap=200)
        self.summary_chunks = summary_splitter.split_documents([combined_doc])
        print(f"summary_chunks: {len(self.summary_chunks)}")

    def retrieve(self, query: str, top_k: int = 5):
        """
        Retrieve relevant documents from the vector store based on a query.
        
        Args:
            query (str): The search query.
            top_k (int): The number of top results to return.
        
        Returns:
            list: A list of retrieved documents.
        """
        results = self.db.similarity_search(query, k=top_k)
        return "\n\n".join([doc.page_content for doc in results])


    def cleanup(self):
        """
        Clean up the Weaviate client connection.
        """
        self.weaviate_client.close()
        print("Weaviate client connection closed.")
    