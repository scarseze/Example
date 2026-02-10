FROM python:3.11-slim

WORKDIR /app

# Install system dependencies (for lxml, build tools)
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    libxml2-dev \
    libxslt-dev \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements first to leverage Docker cache
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY . .

# Set PYTHONPATH to include current directory
ENV PYTHONPATH=/app

# Default command (can be overridden in docker-compose)
CMD ["python", "main.py"]
