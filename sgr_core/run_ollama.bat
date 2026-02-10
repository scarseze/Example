@echo off
echo Starting SGR Core Agent with OLLAMA...

:: Configure for Local Ollama
set LLM_BASE_URL=http://localhost:11434/v1
set LLM_API_KEY=ollama

:: CHANGE THIS to your installed model (e.g., llama3, mistral, qwen2.5-coder)
set LLM_MODEL=llama3

echo Model: %LLM_MODEL%
echo Base URL: %LLM_BASE_URL%

python main.py
pause
