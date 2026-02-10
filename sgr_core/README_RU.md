# SGR Core (Schema-Guided Reasoning Agent)

**Автономный AI-агент с модульной архитектурой навыков (Skills), интеграцией RAG и корпоративной безопасностью.**

![Python](https://img.shields.io/badge/Python-3.11-blue) ![Docker](https://img.shields.io/badge/Docker-Enabled-blue) ![Architecture](https://img.shields.io/badge/Architecture-Event--Driven-orange) ![Persona](https://img.shields.io/badge/Persona-Buratino-yellow)

## Обзор

SGR Core — это система искусственного интеллекта, демонстрирующая **продвинутые паттерны оркестрации** для больших языковых моделей (LLM). В отличие от простых чат-ботов, SGR Core использует паттерн **Router-Broker** для анализа намерений пользователя и делегирования задач специализированным "Навыкам" (микро-агентам).

**Новое в v2.0:**
*   **Персона "Буратино"**: Умный и жизнерадостный характер ассистента.
*   **Голосовой интерфейс**: Нативная поддержка голосовых сообщений (через Groq Whisper).
*   **Единое ядро**: Функции персонального ассистента интегрированы в основной движок.

## Ключевые Технологии

**Стек:**
*   **Язык**: Python 3.11 (Type Hinted)
*   **Контейнеризация**: Docker & Docker Compose
*   **LLM Интеграция**: OpenAI SDK (совместим с DeepSeek V3, Groq, Ollama)
*   **Платформы**: Telegram (Голос/Текст), Max Messenger (Corp), Web UI (Chainlit)
*   **Векторная БД**: Qdrant (Self-hosted)

## Архитектура

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

## Реализованные функции

1.  **Мышление и Маршрутизация**: Агент не просто угадывает — он генерирует структурированный "План реализации" (JSON) перед действием.
2.  **Голосовое взаимодействие**: Отправляйте голосовые сообщения "Буратино" и получайте умные ответы (текстом или файлами).
3.  **Global RAG**:
    *   Асинхронный скрипт загрузки данных (`populate_rag.py`) со скоростью **0.1-0.5 файлов/сек**.
    *   Гибридный поиск (Keyword + Vector) через Qdrant.
4.  **Security Guardian**:
    *   Regex-фаервол, блокирующий опасные Linux-команды (`rm -rf`, `nc`, `env`).
    *   Изоляция сети Docker (Proxy Pattern) для защиты API ключей.
5.  **Офисная автоматизация**:
    *   Генерация сложных отчетов (`.docx`) и презентаций (`.pptx`) по естественному запросу.

## Начало работы

### Требования
*   Docker Desktop
*   Python 3.11+
*   (Опционально) Ollama для локального инференса

### Установка

1.  Клонируйте репозиторий:
    ```bash
    git clone https://github.com/your-username/sgr-core.git
    cd sgr-core
    ```
2.  Настройте окружение:
    ```bash
    cp .env.example .env
    # Впишите ваши ключи в .env (OPENAI_API_KEY, GROQ_API_KEY и т.д.)
    ```
3.  Запуск единой системы (API + Telegram):
    ```bash
    ./start_all.bat
    ```
    Или через Docker:
    ```bash
    docker-compose up -d --build
    ```
