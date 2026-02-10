import os
import json
import httpx
from qdrant_client import QdrantClient
from qdrant_client.models import Distance, VectorParams, PointStruct

# Конфигурация Qdrant
QDRANT_HOST = os.getenv("QDRANT_HOST", "localhost")
QDRANT_PORT = int(os.getenv("QDRANT_PORT", 6333))
COLLECTION_NAME = "cbr_reports"
OLLAMA_HOST = os.getenv("OLLAMA_HOST", "http://localhost:11434")

def init_qdrant():
    client = QdrantClient(host=QDRANT_HOST, port=QDRANT_PORT)
    return client

def create_collection_if_not_exists(client):
    collections = client.get_collections().collections
    exists = any(c.name == COLLECTION_NAME for c in collections)
    
    if not exists:
        print(f"Creating collection {COLLECTION_NAME}...")
        client.create_collection(
            collection_name=COLLECTION_NAME,
            vectors_config=VectorParams(size=768, distance=Distance.COSINE), # Размер зависит от эмбеддинг модели
        )
        print("Collection created.")
    else:
        print(f"Collection {COLLECTION_NAME} already exists.")

import requests

class OllamaEmbedder:
    def __init__(self, model="nomic-embed-text", base_url=OLLAMA_HOST):
        self.model = model
        self.api_url = f"{base_url}/api/embeddings"

    def embed_query(self, text):
        try:
            response = requests.post(self.api_url, json={
                "model": self.model,
                "prompt": text
            })
            if response.status_code == 200:
                return response.json().get("embedding")
            else:
                print(f"Error from Ollama: {response.text}")
                return None
        except Exception as e:
            print(f"Connection error to Ollama: {e}")
            return None

class AsyncOllamaEmbedder:
    def __init__(self, model="nomic-embed-text", base_url=OLLAMA_HOST):
        self.model = model
        self.api_url = f"{base_url}/api/embeddings"

    async def embed_query(self, text, client: httpx.AsyncClient = None):
        payload = {
            "model": self.model,
            "prompt": text
        }
        try:
            # Используем переданный клиент или создаем временный
            if client:
                response = await client.post(self.api_url, json=payload)
            else:
                async with httpx.AsyncClient() as local_client:
                    response = await local_client.post(self.api_url, json=payload)
            
            if response.status_code == 200:
                return response.json().get("embedding")
            else:
                print(f"Error from Ollama: {response.text}")
                return None
        except Exception as e:
            print(f"Connection error to Ollama: {e}")
            return None

def upload_chunks(client, chunks_file):
    print(f"Loading chunks from {chunks_file}...")
    with open(chunks_file, 'r', encoding='utf-8') as f:
        chunks = json.load(f)
        
    points = []
    embedder = OllamaEmbedder(model="nomic-embed-text") # Или другая модель
    
    print("Generating embeddings via Ollama...")
    
    for i, chunk in enumerate(chunks):
        content = chunk.get("content", "")
        vector = embedder.embed_query(content)
        
        if vector:
            points.append(PointStruct(
                id=i, 
                vector=vector,
                payload=chunk
            ))
        else:
            print(f"Skipping chunk {i} due to embedding error")
        
        if i % 10 == 0:
            print(f"Processed {i}/{len(chunks)} chunks")
        
    if points:
        client.upsert(
            collection_name=COLLECTION_NAME,
            wait=True,
            points=points
        )
        print(f"Uploaded {len(points)} chunks to Qdrant.")

if __name__ == "__main__":
    # Пример использования
    # Нужно сначала запустить cbr_etl.py чтобы создать JSON
    client = init_qdrant()
    create_collection_if_not_exists(client)
    
    # Ищем файлы JSON с чанками
    data_dir = "data"
    if os.path.exists(data_dir):
        for filename in os.listdir(data_dir):
            if filename.startswith("chunks_") and filename.endswith(".json"):
                 upload_chunks(client, os.path.join(data_dir, filename))
