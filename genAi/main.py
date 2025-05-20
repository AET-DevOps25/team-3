from fastapi import FastAPI
import os
import weaviate

# Initialize Weaviate client
weaviate_url = os.getenv("WEAVIATE_URL", "http://weaviate:8082")
client = weaviate.Client(url=weaviate_url)

app = FastAPI(
    title="tutor",
    openapi_tags=[
        {
            "name": "Health",
            "description": "Endpoints to check the health of the service.",
        },
        {
            "name": "Pool",
            "description": "Endpoints to manage data collections.",
        },
        {
            "name": "Search",
            "description": "Endpoints for searching within data collections.",
        },
        {"name": "Ingestion", "description": "Endpoints to start ingestion processes."},
    ],
)


@app.get("/health")
async def health_check():
    """Check the health of the service and its dependencies."""
    try:
        # Check if Weaviate is accessible
        client.is_ready()
        return {"status": "healthy", "weaviate": "connected"}
    except Exception as e:
        return {"status": "unhealthy", "error": str(e)}
