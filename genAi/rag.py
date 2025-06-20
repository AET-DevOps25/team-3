import weaviate
from langchain_community.document_loaders import TextLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_weaviate.vectorstores import WeaviateVectorStore
from langchain_cohere import CohereEmbeddings
from dotenv import load_dotenv
import os

# Load documents
loader = TextLoader("dummy_knowledge2.txt")
documents = loader.load()

# Split documents into smaller chunks
text_splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=200)
split_documents = text_splitter.split_documents(documents)
# Initialize embeddings model
load_dotenv()
embeddings_model = CohereEmbeddings(model="embed-english-light-v3.0", cohere_api_key=os.getenv("COHERE_API_KEY"))

# Initialize Weaviate client
weaviate_client = weaviate.connect_to_local()
db = WeaviateVectorStore.from_documents(split_documents, embeddings_model, client=weaviate_client)

def retrieve(query: str, top_k: int = 5):
    """
    Retrieve relevant documents from the vector store based on a query.
    
    Args:
        query (str): The search query.
        top_k (int): The number of top results to return.
    
    Returns:
        list: A list of retrieved documents.
    """
    results = db.similarity_search(query, k=top_k)
    return "\n\n".join([doc.page_content for doc in results])


def rag_cleanup():
    """
    Clean up the Weaviate client connection.
    """
    weaviate_client.close()
    print("Weaviate client connection closed.")