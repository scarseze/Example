# SGR Core (Schema-Guided Reasoning Agent)

**An autonomous AI agent with modular Skill Architecture, RAG integration, and Enterprise-grade security.**

![Python](https://img.shields.io/badge/Python-3.11-blue) ![Docker](https://img.shields.io/badge/Docker-Enabled-blue) ![Architecture](https://img.shields.io/badge/Architecture-Event--Driven-orange) ![Persona](https://img.shields.io/badge/Persona-Buratino-yellow)

## Overview

SGR Core is a proof-of-concept AI system designed to demonstrate **advanced orchestration patterns** for Large Language Models (LLMs). Unlike simple chatbots, SGR Core uses a **Router-Broker pattern** to analyze user intent and delegate execution to specialized "Skills" (Micro-Agents).

**New in v2.0:**
*   **Persona "Buratino"**: A smart, cheerful assistant personality.
*   **Voice Interface**: Native support for Voice Messages (via Groq Whisper).
*   **Unified Core**: Integrated Personal Assistant features directly into the main engine.

## Key Technologies

**Core Stack:**
*   **Language**: Python 3.11 (Type Hinted)
*   **Containerization**: Docker & Docker Compose
*   **LLM Integration**: OpenAI SDK (compatible with DeepSeek V3, Groq, Ollama)
*   **Platforms**: Telegram (Voice/Text), Max Messenger (Corp), Web UI (Chainlit)
*   **Vector DB**: Qdrant (Self-hosted)

## Architecture

```mermaid
graph TD
    User[User Interface] -->|Voice/Text| Telegram[Telegram Bot]
    Telegram -->|Voice Audio| Whisper[Groq Transcription]
    Telegram -->|Text| Core[Core Engine (Router)]
    
    Whisper -->|Transcribed Text| Core
    
    Core -->|1. Reason (Buratino Persona)| Brain[LLM (DeepSeek/Groq)]
    Brain -->|2. Select Skill| Core
    
    Core -->|3. Execute| SkillRegistry
    
    subgraph Skills [Modular Skills]
        Portfolio[Finance Analyst]
        RAG[Knowledge Base (Qdrant)]
        Calendar[Calendar Agent]
        Office[Report Generator]
    end
    
    SkillRegistry --> Skills
    Skills -->|Context| RAG
```

## Features implemented

1.  **Thinking & Routing**: The agent doesn't just guess; it generates a structured "Implementation Plan" before acting.
2.  **Voice Interaction**: Send voice messages to "Buratino" and get intelligent text/file responses.
3.  **Global RAG**:
    *   Asynchronous ingestion script (`populate_rag.py`) hitting **0.1-0.5 files/sec** throughput.
    *   Hybrid search (Keyword + Vector) via Qdrant.
4.  **Security Guardian**:
    *   Regex-based firewall blocking dangerous Linux commands (`rm -rf`, `nc`, `env`).
    *   Docker Network Isolation (Proxy Pattern) to protect API Keys.
5.  **Office Automation**:
    *   Direct generation of complex reports (`.docx`) and presentations (`.pptx`) from natural language.

## Getting Started

### Prerequisites
*   Docker Desktop
*   Python 3.11+
*   (Optional) Ollama for local inference

### Installation

1.  Clone the repository:
    ```bash
    git clone https://github.com/your-username/sgr-core.git
    cd sgr-core
    ```
2.  Configure Environment:
    ```bash
    cp .env.example .env
    # Edit .env with your keys (OPENAI_API_KEY, GROQ_API_KEY, etc.)
    ```
3.  Run the Unified System (API + Telegram):
    ```bash
    ./start_all.bat
    ```
    Or via Docker:
    ```bash
    docker-compose up -d --build
    ```
