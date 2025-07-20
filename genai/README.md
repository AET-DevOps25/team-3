# 🧠 GenAI & RAG Pipeline

This module implements the core GenAI capabilities for **Study Mate**, leveraging Retrieval-Augmented Generation (RAG) and LangChain to enable document-aware responses, summaries, flashcards, and quizzes.


## 🔍 What It Does

When a user uploads a `.pdf` or `.txt` document, the system:

- ✅ Parses and splits the content into meaningful chunks
- 🧠 Embeds content using HuggingFace (MiniLM) and stores in **Weaviate**
- 💬 Supports document-specific chat using RAG
- ✍️ Generates:
  - Structured **summaries** (Markdown)
  - **Flashcards** (difficulty-tagged)
  - **Quizzes** (MCQ and short-answer)


## 📁 Key Files

| File | Description |
|------|-------------|
| `llm.py` | Manages all GenAI functionality: chat, summarization, flashcards, quiz generation |
| `rag.py` | Handles ingestion, chunking, metadata, vector embedding, and retrieval via Weaviate |
| `chains.py` | Defines custom LangChain chains to generate structured outputs (flashcards, quizzes) |


## 🧠 Architecture Overview

- 🔗 **LangChain** for LLM orchestration
- 📚 **Weaviate** stores two types of chunks:
  - `RAGChunksIndex`: Vectorized, small chunks for semantic search
  - `GenerationChunksIndex`: Larger, plain text chunks for generative tasks (e.g., summaries)
- 🤗 **HuggingFace** MiniLM used for embeddings
- 🌐 **Open WebUI-compatible API** (LLaMA 3) for LLM calls


## 🗂 Workflow

1. **Load document**  
   → via `StudyLLM.load_document(doc_name, path, user_id)`

2. **Ingest chunks**  
   → Embedded RAG chunks go to `RAGChunksIndex`  
   → Generation chunks go to `GenerationChunksIndex`

3. **Chat**  
   → Queries are filtered by `user_id` and optionally `doc_name`  
   → Top-k relevant chunks retrieved from Weaviate and passed to the LLM

4. **Summarize / Flashcards / Quiz**  
   → Uses larger plain-text chunks stored per document  
   → LangChain’s **map-reduce** pattern **parallelized** to generate structured output


## 🧪 Available Features

| Feature | Method |
|--------|--------|
| Load Document | `load_document(doc_name, path, user_id)` |
| RAG Chat | `prompt(prompt, user_id)` |
| Summarize | `summarize(document_name, user_id)` |
| Flashcards | `generate_flashcards(document_name, user_id)` |
| Quiz | `generate_quiz(document_name, user_id)` |
| Cleanup  (see below) | `cleanup()` |

## 📌 Notes

- Only `.pdf` and `.txt` documents are supported.

## 📦 Dependencies

- `langchain`
- `langchain-openai`
- `langchain-huggingface`
- `langchain-community`
- `weaviate-client`
- `PyMuPDF`, `dotenv`, `asyncio`, etc.


## 🧹 Cleanup

Call `StudyLLM.cleanup()` to close the Weaviate client connection properly.


## 📄 License

MIT — see [LICENSE](../LICENSE)
