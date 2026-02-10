# CBR RAG & ETL Backend

**Высокопроизводительный асинхронный бэкенд для финансовой аналитики и векторного поиска.**

![Python](https://img.shields.io/badge/Python-3.11-blue) ![FastAPI](https://img.shields.io/badge/FastAPI-Async-green) ![Docker](https://img.shields.io/badge/Docker-Ready-blue) ![Qdrant](https://img.shields.io/badge/DB-Qdrant-red)

## 📊 Обзор

Этот микросервис обеспечивает фундамент данных для Агента-Аналитика. Он управляет сложными ETL-пайплайнами (данные ЦБ РФ), обеспечивает надежную векторизацию через Ollama и предоставляет быстрый Async API.

**Ключевые возможности (v2.0):**
*   **Полная Асинхронность**: Построен на `FastAPI` и `httpx`. Неблокирующий ввод-вывод для высокой пропускной способности.
*   **Docker Native**: Полная поддержка `docker-compose` с конфигурацией через переменные окружения (`12-factor` app).
*   **Надежный ETL**: Умная распаковка устаревших архивов фин. отчетности (RAR/DBF) с фоллбэком на `7z` для совместимости с Linux.

## 🛠️ Стек Технологий

*   **API Фреймворк**: FastAPI, Uvicorn
*   **Асинхронность**: `httpx`, `asyncio`
*   **Векторная БД**: Qdrant
*   **Эмбеддинги**: Ollama (`nomic-embed-text`)
*   **Обработка данных**: Pandas, dbfread, rarfile (с поддержкой subprocess)

## 🚀 Начало работы

### Требования

*   Docker & Docker Compose

### Запуск в Docker (Рекомендуется)

1.  Перейдите в корень проекта:
    ```bash
    cd cbr_rag
    ```
2.  Запустите сервисы:
    ```bash
    docker-compose up -d --build
    ```
    Запустятся API (порт 8000), Qdrant (6333) и Ollama (11434).

3.  (Первый запуск) Скачайте модели Ollama:
    ```bash
    docker-compose exec ollama ollama pull nomic-embed-text
    ```

### Переменные Окружения

*   `QDRANT_HOST`: Хост Qdrant (по умолчанию: `localhost` или `qdrant` в docker)
*   `OLLAMA_HOST`: URL Ollama API (по умолчанию: `http://localhost:11434` или `http://ollama:11434` в docker)

## 🧬 API Эндпоинты

*   `GET /api/analyze?reg_number=1481`: Синхронный анализ банка по рег. номеру.
*   `POST /search`: Семантический поиск по финансовым документам.
*   `GET /graphql`: GraphQL эндпоинт для сложных запросов.

