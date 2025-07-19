import weaviate
from weaviate.classes.query import Filter
from langchain_community.document_loaders import PyMuPDFLoader, TextLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_weaviate.vectorstores import WeaviateVectorStore
from langchain_core.documents import Document
from dotenv import load_dotenv
import os
from helpers import delete_document
import logging
from langchain_huggingface import HuggingFaceEmbeddings

# Setup shared embeddings model
load_dotenv()
embeddings_model = HuggingFaceEmbeddings(model_name="all-MiniLM-L6-v2")

# Disable Huggingface's tokenizer parallelism (avoid deadlocks caused by process forking in langchain)
os.environ["TOKENIZERS_PARALLELISM"] = "false"


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
        logging.info(f"Loading PDF file: {doc_path}")
        return PyMuPDFLoader(doc_path)
    elif doc_path.endswith('.txt'):
        logging.info(f"Loading text file: {doc_path}")
        return TextLoader(doc_path)
    else:
        raise ValueError("Unsupported file type. Please provide a .pdf or .txt file.")

class RAGHelper:
    """
    A helper for the retrieval stage of the RAG pipeline.
    """
    
    def __init__(self, doc_path: str):
        # Initialize Weaviate client first
        weaviate_host = os.getenv("WEAVIATE_HOST", "localhost")
        weaviate_port = os.getenv("WEAVIATE_PORT", "8083")
        # Use custom connection with HTTP only since gRPC is not exposed
        self.weaviate_client = weaviate.connect_to_custom(
            http_host=weaviate_host,
            http_port=int(weaviate_port),
            http_secure=False,
            grpc_host=weaviate_host,
            grpc_port=50051,
            grpc_secure=False,
            skip_init_checks=True
        )
        
        # Load documents
        try:
            loader = _get_loader(doc_path)
        except Exception as e:
            raise ValueError(f"Error loading document: {e}")
        
        documents = loader.load()
        delete_document(doc_path) # Delete uploaded document after loading
        doc_metadata = {
            "source": doc_path,
        }
        # Split document into smaller chunks
        
        split_documents = self._split_and_attach_metadata(documents, doc_metadata)
        
        # Initialize embeddings model

        # Initialize Weaviate vector store
        self.db = WeaviateVectorStore.from_documents(split_documents, embeddings_model, client=self.weaviate_client, index_name="UserDocsIndex")
        self.doc_path = doc_path # Store the document path for retrieval filtering
        # Split documents for summarization, flashcards, and quiz generation.
        ## combine all documents into a single text (avoid 1 document per page)
        full_text = "\n\n".join([doc.page_content for doc in documents])
        combined_doc = Document(page_content=full_text)
        self.summary_chunks = self._split_and_attach_metadata(
            [combined_doc],
            doc_metadata,
            chunk_size=4000,
            chunk_overlap=200
        )


    def _split_and_attach_metadata(self,
                                   documents: list[Document],
                                   metadata: dict,
                                   chunk_size: int = 1000,
                                   chunk_overlap: int = 200):
        """
        Split documents into smaller chunks and attach metadata.
        
        Args:
            documents (list[Document]): List of documents to split.
            metadata (dict): Metadata to attach to each document chunk.
        
        Returns:
            list[Document]: List of document chunks with attached metadata.
        """
        splitter = RecursiveCharacterTextSplitter(chunk_size=chunk_size, chunk_overlap=chunk_overlap)
        split_docs = splitter.split_documents(documents)
        for i, doc in enumerate(split_docs):
            doc.metadata.update(metadata)
            doc.metadata['chunk_index'] = i
        return split_docs

    def retrieve(self, query: str, top_k: int = 5):
        """
        Retrieve relevant documents from the vector store based on a query.
        
        Args:
            query (str): The search query.
            top_k (int): The number of top results to return.
        
        Returns:
            list: A list of retrieved documents.
        """
        doc_filter = Filter.by_property("source").equal(self.doc_path)
        results = self.db.similarity_search(query, k=top_k, filters=doc_filter)
        return "\n\n".join([doc.page_content for doc in results])


    def cleanup(self):
        """
        Clean up the Weaviate client connection.
        """
        self.weaviate_client.close()
        logging.info("Weaviate client connection closed.")
    