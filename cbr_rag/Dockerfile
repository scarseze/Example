FROM python:3.11-slim

# Установка системных зависимостей
# unrar/p7zip нужен для распаковки архивов в cbr_etl.py, если он будет запускаться здесь
RUN apt-get update && apt-get install -y \
    p7zip-full \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Установка Python зависимостей
# В идеале использовать requirements.txt, здесь перечислен весь стек проекта
RUN pip install --no-cache-dir \
    fastapi uvicorn httpx qdrant-client pydantic \
    strawberry-graphql okama pandas requests rarfile dbfread

# Копирование кода проекта
COPY . .

# Порт API
EXPOSE 8000

# Запуск сервера
CMD ["uvicorn", "server:app", "--host", "0.0.0.0", "--port", "8000"]