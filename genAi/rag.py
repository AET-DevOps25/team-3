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

    def __init__(self, user_id: str, doc_path: str):
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

        self.user_id = user_id
        self.doc_path = doc_path # Store the document path for filtering

        # Ensure Weaviate collections (classes) exist with correct configurations
        self._create_weaviate_collections()

        # Load documents from the specified path
        try:
            loader = _get_loader(doc_path)
            documents = loader.load()
            logging.info(f"Successfully loaded {len(documents)} pages/sections from {doc_path}")
        except Exception as e:
            logging.error(f"Error loading document {doc_path}: {e}")
            raise ValueError(f"Error loading document: {e}")

        # Delete the uploaded document after loading
        delete_document(doc_path)

        doc_metadata = {"user_id": user_id, "source": doc_path}

        # --- Handle RAG Chunks (embedded) ---
        # Split document into smaller chunks for RAG retrieval
        rag_split_documents = self._split_and_attach_metadata(documents, doc_metadata)
        # Initialize WeaviateVectorStore for RAG chunks
        self.rag_db = WeaviateVectorStore.from_documents(
            rag_split_documents,
            embeddings_model,
            client=self.weaviate_client,
            index_name="RAGChunksIndex"
        )

        # --- Handle Generation Chunks (plain text, no embedding) ---
        # Combine all documents into a single text for generation chunks
        full_text = "\n\n".join([doc.page_content for doc in documents])
        combined_doc = Document(page_content=full_text)
        # Split for summarization, flashcards, and quiz generation (larger chunks)
        generation_chunks = self._split_and_attach_metadata(
            [combined_doc],
            doc_metadata,
            chunk_size=4000,
            chunk_overlap=200
        )
        # Store generation chunks
        self._store_generation_chunks(generation_chunks)

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

            # SummaryChunksIndex: For larger, plain text chunks, no-embedding chunks (for summary, etc.)
            if "SummaryChunksIndex" not in existing_collections:
                collections.create(
                    name="SummaryChunksIndex",
                    properties=[
                        Property(name="text", data_type=DataType.TEXT),
                        Property(name="user_id", data_type=DataType.TEXT),
                        Property(name="source", data_type=DataType.TEXT),
                        Property(name="chunk_index", data_type=DataType.INT),
                    ],
                    vectorizer_config=Configure.Vectorizer.none()
                )
                
    def _store_generation_chunks(self, chunks: list[Document]):
        summary_collection = self.weaviate_client.collections.get("SummaryChunksIndex")
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
                                   chunk_size: int = 1000,
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

    def retrieve(self, query: str, top_k: int = 5) -> str:
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
        doc_filter = Filter.by_property("user_id").equal(self.user_id) & \
                     Filter.by_property("source").equal(self.doc_path)
        
        results = self.rag_db.similarity_search(query, k=top_k, filters=doc_filter)
        
        if not results:
            logging.warning("No documents found for the given query and filters.")
            return ""
            
        retrieved_content = "\n\n".join([doc.page_content for doc in results])
        return retrieved_content

    def get_generation_chunks(self) -> list[Document]:
        """
        Retrieves the larger, plain text chunks stored for generation purposes.
        Filters by user_id and document source.

        Returns:
            list[str]: A list of strings, where each string is a generation chunk,
                       sorted by their original chunk index.
        """
        summary_collection = self.weaviate_client.collections.get("SummaryChunksIndex")
        
         # filter for user ID and source doc
        doc_filter = Filter.by_property("user_id").equal(self.user_id) & \
                     Filter.by_property("source").equal(self.doc_path)
        # Define the query with filtering and properties to retrieve
        
        response = summary_collection.query.fetch_objects(
            return_properties=["text", "chunk_index"],
            filters=doc_filter,
            limit=100 # high enough, should be less than 100
        )

        data = response.objects
        
        if not data:
            logging.warning("No generation chunks found for the specified user and source.")
            return []

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