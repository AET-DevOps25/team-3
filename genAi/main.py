from fastapi import FastAPI


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
        {"name": "Search", "description": "Endpoints for searching within data collections."},
        {"name": "Ingestion", "description": "Endpoints to start ingestion processes."},
    ],
)


