# CBR RAG & ETL Backend

**High-Performance Async Backend for Financial Data Analysis and Vector Search.**

![Python](https://img.shields.io/badge/Python-3.11-blue) ![FastAPI](https://img.shields.io/badge/FastAPI-Async-green) ![Docker](https://img.shields.io/badge/Docker-Ready-blue) ![Qdrant](https://img.shields.io/badge/DB-Qdrant-red)

## Overview

This microservice provides the data foundation for the Financial Analysis Agent. It handles complex ETL pipelines (Central Bank of Russia data), ensures robust vector embedding via Ollama, and exposes a high-performance Async API.

**Key Features (v2.0):**
*   **Fully Asynchronous**: Built on `FastAPI` and `httpx`. Non-blocking I/O for high throughput.
*   **Docker Native**: Full `docker-compose` support with environment-based configuration (`12-factor` app).
*   **Robust ETL**: Smart unpacking of legacy financial archives (RAR/DBF) with fallback to `7z` for Linux compatibility.

## Tech Stack

*   **API Framework**: FastAPI, Uvicorn
*   **Asynchronous I/O**: `httpx`, `asyncio`
*   **Vector Database**: Qdrant
*   **Embeddings**: Ollama (`nomic-embed-text`)
*   **Data Processing**: Pandas, dbfread, rarfile (with subprocess fallback)

## Getting Started

### Prerequisites

*   Docker & Docker Compose

### Running in Docker (Recommended)

1.  Navigate to project root:
    ```bash
    cd cbr_rag
    ```
2.  Start services:
    ```bash
    docker-compose up -d --build
    ```
    This launches API (port 8000), Qdrant (6333), and Ollama (11434).

3.  (First Run) Pull Ollama Models:
    ```bash
    docker-compose exec ollama ollama pull nomic-embed-text
    ```

### Env Variables

*   `QDRANT_HOST`: Hostname of Qdrant (default: `localhost` or `qdrant` in docker)
*   `OLLAMA_HOST`: URL of Ollama API (default: `http://localhost:11434` or `http://ollama:11434` in docker)

## API Endpoints

*   `GET /api/analyze?reg_number=1481`: Get synchronous analysis of a bank.
*   `POST /search`: Semantic search over financial documents.
*   `GET /graphql`: GraphQL endpoint for complex queries.
