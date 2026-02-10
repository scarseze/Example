import os
import json
import re
from typing import Type, TypeVar, Optional, Any
from pydantic import BaseModel, ValidationError
from openai import AsyncOpenAI
import asyncio

T = TypeVar("T", bound=BaseModel)

class LLMService:
    def __init__(self, base_url: str = None, api_key: str = None, model: str = "deepseek-chat"):
        self.api_key = api_key or os.getenv("LLM_API_KEY") or os.getenv("DEEPSEEK_API_KEY")
        self.client = AsyncOpenAI(
            base_url=base_url or os.getenv("LLM_BASE_URL", "https://api.deepseek.com"),
            api_key=self.api_key
        )
        self.model = model or os.getenv("LLM_MODEL", "deepseek-chat")

    async def generate_structured(self, system_prompt: str, user_prompt: str, response_model: Type[T]) -> T:
        """
        Generates a response from the LLM asynchronously and parses it into the given Pydantic model.
        Forces JSON mode if possible, but also cleanses markdown code blocks.
        """
        schema_json = json.dumps(response_model.model_json_schema(), indent=2)
        
        # Enhanced system prompt to enforce JSON
        full_system_prompt = (
            f"{system_prompt}\n\n"
            "You MUST respond with valid JSON strictly conforming to this schema:\n"
            f"```json\n{schema_json}\n```\n"
            "Do not include any text outside the JSON object."
        )

        try:
            response = await self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {"role": "system", "content": full_system_prompt},
                    {"role": "user", "content": user_prompt}
                ],
                response_format={"type": "json_object"} # Try to enforce JSON mode
            )
            
            if not response or not hasattr(response, 'choices') or not response.choices:
                raise ValueError(f"Invalid LLM response: {response}")

            content = response.choices[0].message.content
            return self._parse_json(content, response_model)
            
        except Exception as e:
            # Fallback or re-raise
            print(f"LLM Error: {e}")
            raise

    def _parse_json(self, content: str, model: Type[T]) -> T:
        try:
            # 1. Try direct parse
            data = json.loads(content)
            return model.model_validate(data)
        except json.JSONDecodeError:
            # 2. Try extracting from markdown ```json ... ```
            match = re.search(r"```json(.*?)```", content, re.DOTALL)
            if match:
                clean_content = match.group(1).strip()
                data = json.loads(clean_content)
                return model.model_validate(data)
            else:
                raise ValueError(f"Could not parse JSON from LLM response: {content[:100]}...")
