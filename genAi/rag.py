import weaviate
from weaviate.classes.query import Filter
from langchain_community.document_loaders import PyMuPDFLoader, TextLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_weaviate.vectorstores import WeaviateVectorStore
from langchain_core.documents import Document
from dotenv import load_dotenv
import os
import logging
from langchain_huggingface import HuggingFaceEmbeddings
from weaviate.collections.classes.config import Property, DataType, Vectorizers, Configure
from helpers import delete_document


# Setup shared embeddings model
load_dotenv()

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

    def __init__(self):
        # Initialize Weaviate client
        weaviate_host = os.getenv("WEAVIATE_HOST", "localhost")
        weaviate_port = os.getenv("WEAVIATE_PORT", "8083")
        try:
            self.weaviate_client = weaviate.connect_to_local(
                host=weaviate_host,
                port=int(weaviate_port),
            )
            logging.info(f"Connected to Weaviate at {weaviate_host}:{weaviate_port}")
        except Exception as e:
            logging.error(f"Failed to connect to Weaviate: {e}")
            raise

        # Ensure Weaviate collections (classes) exist with correct configurations
        self._create_weaviate_collections()

        # Initialize WeaviateVectorStore for RAG chunks
        embeddings_model = HuggingFaceEmbeddings(model_name="all-MiniLM-L6-v2")
        self.rag_db = WeaviateVectorStore(
            client=self.weaviate_client,
            embedding=embeddings_model,
            index_name="RAGChunksIndex",
            text_key="text"
        )

    def _create_weaviate_collections(self):
            """
            Ensures the necessary Weaviate collections (classes) exist with the correct schema.
            This method is idempotent.
            """
            collections = self.weaviate_client.collections
            existing_collections = set(collections.list_all().keys())

            # RAGChunksIndex: For embedded chunks, used in chat
            if "RAGChunksIndex" not in existing_collections:
                collections.create(
                    name="RAGChunksIndex",
                    properties=[
                        Property(name="text", data_type=DataType.TEXT),
                        Property(name="user_id", data_type=DataType.TEXT),
                        Property(name="source", data_type=DataType.TEXT),
                        Property(name="chunk_index", data_type=DataType.INT),
                    ],
                    vectorizer_config=Configure.Vectorizer.none() # embedding using HuggingFace model
                )

            # GenerationChunksIndex: For larger, plain text chunks, no-embedding chunks (for summary, etc.)
            if "GenerationChunksIndex" not in existing_collections:
                collections.create(
                    name="GenerationChunksIndex",
                    properties=[
                        Property(name="text", data_type=DataType.TEXT),
                        Property(name="user_id", data_type=DataType.TEXT),
                        Property(name="source", data_type=DataType.TEXT),
                        Property(name="chunk_index", data_type=DataType.INT),
                    ],
                    vectorizer_config=Configure.Vectorizer.none()
                )
                
    def _store_generation_chunks(self, chunks: list[Document]):
        summary_collection = self.weaviate_client.collections.get("GenerationChunksIndex")
        with summary_collection.batch.dynamic() as batch:
            for i, doc in enumerate(chunks):
                batch.add_object(
                    properties={
                        "text": doc.page_content,
                        "user_id": doc.metadata["user_id"],
                        "source": doc.metadata["source"],
                        "chunk_index": doc.metadata["chunk_index"]
                    }
                )
                logging.debug(f"Added generation chunk {i} to batch.")
        logging.info(f"Finished storing {len(chunks)} generation chunks.")


    def _split_and_attach_metadata(self,
                                   documents: list[Document],
                                   metadata: dict,
                                   chunk_size: int = 512,
                                   chunk_overlap: int = 200) -> list[Document]:
        """
        Split documents into smaller chunks and attach metadata.

        Args:
            documents (list[Document]): List of documents to split.
            metadata (dict): Metadata to attach to each document chunk.
            chunk_size (int): The maximum size of each chunk.
            chunk_overlap (int): The overlap between consecutive chunks.

        Returns:
            list[Document]: List of document chunks with attached metadata.
        """
        splitter = RecursiveCharacterTextSplitter(chunk_size=chunk_size, chunk_overlap=chunk_overlap)
        split_docs = splitter.split_documents(documents)
        for i, doc in enumerate(split_docs):
            doc.metadata.update(metadata)
            doc.metadata['chunk_index'] = i
        return split_docs


    async def load_document(self, doc_name: str, path: str, user_id: str):
        """
        Load a document into the RAG system.
        
        Args:
            doc_name (str): The name of the document to load.
            path (str): The path to the document file.
            user_id (str): The ID of the user loading the document.
        """
        try:
            # Load the document using the appropriate loader
            loader = _get_loader(path)
            documents = loader.load()
            logging.info(f"Loaded {len(documents)} pages/sections from {path}")
            
            # Delete the uploaded document after loading
            delete_document(path)

            # Attach metadata and split documents for RAG
            doc_metadata = {"user_id": user_id, "source": doc_name}
            
            # --- Handle RAG Chunks (embedded) ---
            rag_split_documents = self._split_and_attach_metadata(documents, doc_metadata)
            # Asynchronously store in Weaviate for RAG retrieval
            await self.rag_db.aadd_documents(rag_split_documents)
            logging.info(f"Stored {len(rag_split_documents)} RAG chunks for user {user_id}.")
            # --- Handle Generation Chunks (plain text, no embedding) ---
            # Combine all documents into a single text for generation chunks
            full_text = "\n\n".join([doc.page_content for doc in documents])
            combined_doc = Document(page_content=full_text)
            # Split for summarization, flashcards, and quiz generation (larger chunks)
            generation_chunks = self._split_and_attach_metadata(
                [combined_doc],
                doc_metadata,
                chunk_size=4096,
                chunk_overlap=200
            )
            # Store generation chunks
            self._store_generation_chunks(generation_chunks)
            # Log successful document load
            logging.info(f"Document {doc_name} loaded successfully for user {user_id}.")
            return f"Document {doc_name} loaded successfully."
        except Exception as e:
            error_msg = f"Failed to load document {doc_name} for user {user_id}: {str(e)}"
            logging.error(error_msg)
            raise ValueError(error_msg)

    def retrieve(self, query: str, user_id: str, doc_name: str = None, top_k: int = 5) -> str:
        """
        Retrieve relevant RAG documents from the vector store based on a query.
        Filters by user_id and document source.

        Args:
            query (str): The search query.
            top_k (int): The number of top results to return.

        Returns:
            str: A concatenated string of the page content of the retrieved documents.
        """
        # filter for user ID and source doc
        doc_filter = Filter.by_property("user_id").equal(user_id)
        if doc_name:
            doc_filter = doc_filter & Filter.by_property("source").equal(doc_name)
        
        results = self.rag_db.similarity_search(query, k=top_k, filters=doc_filter)
        
        if not results:
            logging.warning("No documents found for the given query and filters.")
            raise ValueError("No documents found for the given query and filters.")
            
        retrieved_content = "\n\n".join([doc.page_content for doc in results])
        return retrieved_content

    def get_generation_chunks(self, user_id: str, doc_name: str) -> list[Document]:
        """
        Retrieves the larger, plain text chunks stored for generation purposes.
        Filters by user_id and document source.

        Returns:
            list[Document]: A list of documents, where each doc is a generation chunk,
                       sorted by their original chunk index.
        """
        summary_collection = self.weaviate_client.collections.get("GenerationChunksIndex")
        
         # filter for user ID and source doc
        doc_filter = Filter.by_property("user_id").equal(user_id) & \
                     Filter.by_property("source").equal(doc_name)
        # Define the query with filtering and properties to retrieve
        
        response = summary_collection.query.fetch_objects(
            return_properties=["text", "chunk_index"],
            filters=doc_filter,
            limit=100 # high enough, should be less than 100
        )

        data = response.objects
        
        if not data:
            logging.warning("No generation chunks found for the specified user and source.")
            raise ValueError("No generation chunks found for the specified user and source.")

        # Sort by chunk_index to preserve original document order
        sorted_chunks = sorted(data, key=lambda d: d.properties.get("chunk_index", 0))
        return [Document(page_content=chunk.properties["text"]) for chunk in sorted_chunks]

    def cleanup(self):
        """
        Closes the Weaviate client connection.
        """
        if self.weaviate_client:
            self.weaviate_client.close()
            logging.info("Weaviate client connection closed.")