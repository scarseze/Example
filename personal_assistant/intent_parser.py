import os
import json
import logging
from openai import AsyncOpenAI
from dotenv import load_dotenv
import datetime

load_dotenv()
logger = logging.getLogger(__name__)

class IntentParser:
    def __init__(self):
        self.api_key = os.getenv("DEEPSEEK_API_KEY")
        if not self.api_key:
            logger.error("DEEPSEEK_API_KEY is missing")
            raise ValueError("DEEPSEEK_API_KEY is required")
        
        self.client = AsyncOpenAI(
            api_key=self.api_key,
            base_url="https://api.deepseek.com"
        )
        self.model = "deepseek-chat"

    async def extract_intent(self, text: str) -> dict:
        """
        Analyzes text and returns a structured JSON intent.
        """
        now = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        
        system_prompt = f"""You are a smart Personal Assistant.
Current Time: {now}
User Location: Assume Moscow (UTC+3) if not specified.

Your goal is to extract structured actionable data from the user's text.
Supported Intents: 'create_event', 'note', 'unknown'.

Output Format: JSON ONLY. No markdown, no explanations.

Schema for 'create_event':
{{
  "intent": "create_event",
  "summary": "Short title of the event",
  "start_time": "YYYY-MM-DD HH:MM:SS" (Approximate if valid, calculated relative to Current Time),
  "end_time": "YYYY-MM-DD HH:MM:SS" (Default to start + 1 hour if not specified),
  "description": "Full original text or key details"
}}

Schema for 'note':
{{
  "intent": "note",
  "content": "The note content"
}}

Schema for 'unknown':
{{
  "intent": "unknown",
  "reason": "Why it's unclear"
}}

Example Input: "Запиши завтра в 5 вечера встречу с Димой"
Example Output:
{{
  "intent": "create_event",
  "summary": "Встреча с Димой",
  "start_time": "2026-01-19 17:00:00",
  "end_time": "2026-01-19 18:00:00",
  "description": "Запиши завтра в 5 вечера встречу с Димой"
}}
"""

        try:
            response = await self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": text}
                ],
                temperature=0.0
            )
            
            content = response.choices[0].message.content.strip()
            # Cleanup code blocks if present
            if content.startswith("```json"):
                content = content[7:]
            if content.endswith("```"):
                content = content[:-3]
            
            return json.loads(content.strip())

        except Exception as e:
            logger.error(f"DeepSeek analysis failed: {e}")
            return {"intent": "unknown", "reason": str(e)}

if __name__ == "__main__":
    import asyncio
    async def main():
        parser = IntentParser()
        print(await parser.extract_intent("Напомни купить молоко сегодня вечером"))
    asyncio.run(main())
