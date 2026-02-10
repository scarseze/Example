import asyncio
import re
import os
import json
from io import StringIO
from contextlib import redirect_stdout
from typing import Optional, Callable, Dict, Any, Type
from dataclasses import dataclass, field
from abc import ABC, abstractmethod

from skills.base import BaseSkill
from skills.rlm.schema import RLMInput
from core.state import AgentState
import httpx # Required by original code

# --- RLM Core Classes (Adapted from src/rlm.py) ---

class LLMProvider(ABC):
    @abstractmethod
    async def query(self, prompt: str, system: Optional[str] = None) -> str: pass
    
    @property
    @abstractmethod
    def name(self) -> str: pass

class DeepSeekProvider(LLMProvider):
    def __init__(self, api_key: str):
        self.api_key = api_key
        self.client = httpx.AsyncClient(timeout=120.0)
        self.base_url = "https://api.deepseek.com"
        self.model = "deepseek-chat"

    async def query(self, prompt: str, system: Optional[str] = None) -> str:
        messages = []
        if system: messages.append({"role": "system", "content": system})
        messages.append({"role": "user", "content": prompt})
        
        try:
            response = await self.client.post(
                f"{self.base_url}/chat/completions",
                headers={"Authorization": f"Bearer {self.api_key}"},
                json={"model": self.model, "messages": messages, "max_tokens": 4096}
            )
            response.raise_for_status()
            return response.json()["choices"][0]["message"]["content"]
        except Exception as e:
            return f"Error: {e}"

    @property
    def name(self) -> str: return "DeepSeek"

@dataclass
class RLMConfig:
    max_iterations: int = 20
    max_output_chars: int = 50000
    sub_call_limit: int = 50
    timeout_seconds: int = 300

@dataclass
class IterationRecord:
    step: int
    code: Optional[str]
    output: str
    error: Optional[str] = None

class RLMOrchestrator:
    # Regex patterns from original code
    CODE_PATTERN = re.compile(r"```(?:repl|python)\n([\s\S]*?)```")
    FINAL_PATTERN = re.compile(r"FINAL\(([\s\S]*?)\)")
    FINAL_VAR_PATTERN = re.compile(r"FINAL_VAR\((\w+)\)")

    def __init__(self, config: RLMConfig, provider: LLMProvider):
        self.config = config
        self.provider = provider
        self.context_store = {}
        self.sub_call_count = 0

    def _build_system_prompt(self, context_len: int) -> str:
        return f"""You are a Python RLM (Recursive Language Model) engine.
Your GOAL is to answer the user query by writing and executing Python key.
You have a variable `context` loaded in memory (len={context_len}).

CRITICAL INSTRUCTIONS:
1. The text is ALREADY in `context`. DO NOT try to read files.
2. DO NOT reply with natural language. You MUST write code in ```repl``` blocks.
3. If you need to analyze the text, split `context` and use `llm_query(chunk)`.
4. Output final answer using `FINAL(text)`.

Example Step 1:
```repl
print(context[:100])
print(len(context))
```
"""

    def _build_iteration_prompt(self, query: str, history: list) -> str:
        prompt = [f"TASK: {query}\n"]
        if history:
            prompt.append("PREVIOUS STEPS:")
            for h in history[-3:]: # Limit history context
                prompt.append(f"\n[Step {h.step}]")
                if h.code: prompt.append(f"Code:\n```repl\n{h.code}\n```")
                prompt.append(f"Output:\n{h.output}" if not h.error else f"Error: {h.error}")
        prompt.append("\nWrite next step code in ```repl block or FINAL().")
        return "\n".join(prompt)

    def _create_llm_query_function(self):
        # Synchronous wrapper for async call (since exec is sync)
        def llm_query(prompt: str) -> str:
            self.sub_call_count += 1
            if self.sub_call_count > self.config.sub_call_limit:
                return "Error: Sub-call limit exceeded"
            
            if len(prompt) > 30000:
                print(f"[RLM Warning] Attempted to send {len(prompt)} chars. Blocking to force chunking.")
                return f"SYSTEM ERROR: The prompt is too long ({len(prompt)} chars). You MUST split the 'context' variable into smaller chunks (max 20k chars) and process them sequentially or in a loop. Do not send the whole context at once."

            print(f"[RLM Sub-Call] Sending {len(prompt)} chars to LLM...")
            
            # SAFE ASYNC-TO-SYNC BRIDGE
            try:
                loop = asyncio.get_event_loop()
                # ... (rest of logic)
                if loop.is_running():
                    future = asyncio.run_coroutine_threadsafe(self.provider.query(prompt), loop)
                    return future.result()
                else:
                    return loop.run_until_complete(self.provider.query(prompt))
            except Exception as e:
                return f"LLM Call Failed: {e}"
        return llm_query

    # Simplified execution logic
    async def process(self, query: str, context: str) -> str:
        self.context_store = {"context": context}
        self.sub_call_count = 0
        
        # PRIMING THE LLM: 
        # We inject a fake "Step 0" where we "verified" the context exists.
        # This forces the LLM to accept the environment state.
        history = [
            IterationRecord(
                step=0, 
                code=f"print(f'Context Size: {{len(context)}} chars')",
                output=f"Context Size: {len(context)} chars",
                error=None
            )
        ]
        
        system_prompt = self._build_system_prompt(len(context))
        
        for i in range(self.config.max_iterations):
            prompt = self._build_iteration_prompt(query, history)
            response = await self.provider.query(prompt, system_prompt)
            
            # 1. Execute Code First
            code_match = self.CODE_PATTERN.search(response)
            output = ""
            error = None
            code = None

            if code_match:
                code = code_match.group(1)
                
                # Execute Code
                output_capture = StringIO()
                env = {
                    "context": context,
                    "re": re, "json": json,
                    "llm_query": self._create_llm_query_function(), 
                    **self.context_store
                }
                
                try:
                    with redirect_stdout(output_capture):
                        exec(code, env)
                    
                    # Update store
                    for k, v in env.items():
                        if k not in ("context", "re", "json", "llm_query") and not k.startswith("_"):
                            self.context_store[k] = v
                except Exception as e:
                    error = str(e)
                
                output = output_capture.getvalue()
                # Truncate
                if len(output) > 1000: output = output[:500] + "\n...[TRUNCATED]...\n" + output[-500:]

            # 2. Check Final (After code has messed with store)
            final_match = self.FINAL_PATTERN.search(response)
            if final_match: return final_match.group(1)
            
            final_var_match = self.FINAL_VAR_PATTERN.search(response)
            if final_var_match:
                var = final_var_match.group(1)
                val = self.context_store.get(var)
                if val is not None:
                    return str(val)
                else:
                    # Treat missing var as an error, NOT a return
                    err_msg = f"System Error: You tried to return FINAL_VAR({var}), but variable '{var}' is not defined in memory. Define it first!"
                    if error:
                        error += "\n" + err_msg
                    else:
                        error = err_msg
            
            # 3. Log History
            if code or error:
                 history.append(IterationRecord(i+1, code, output, error))
            else:
                 history.append(IterationRecord(i+1, None, "No code block or final answer found. Please write code in ```repl``` block."))
            
            # Debug print
            print(f"[RLM Step {i+1}] Code: {bool(code)} | Error: {bool(error)} | Final: {bool(final_match or final_var_match)}")
            
        return "Max iterations reached without final answer."

# --- Skill Wrapper ---

class RLMSkill(BaseSkill):
    name: str = "rlm_skill"
    description: str = "Recursive Language Model for deep analysis of large texts, PDF files, and codebases."

    @property
    def input_schema(self) -> Type[RLMInput]:
        return RLMInput

    async def execute(self, params: RLMInput, state: AgentState) -> str:
        # 1. Resolve Context
        context = ""
        if params.context_text:
            context = params.context_text
        elif params.context_file_path:
            if os.path.exists(params.context_file_path):
                file_ext = os.path.splitext(params.context_file_path)[1].lower()
                
                if file_ext == '.pdf':
                    try:
                        import pypdf
                        with open(params.context_file_path, "rb") as f:
                            reader = pypdf.PdfReader(f)
                            text_list = []
                            for page in reader.pages:
                                text_list.append(page.extract_text() or "")
                            context = "\n".join(text_list)
                            print(f"[{self.name}] Extracted {len(context)} chars from PDF.")
                    except ImportError:
                        return "Error: 'pypdf' library is missing. Rebuild container."
                    except Exception as e:
                        return f"Error reading PDF: {e}"
                else:
                    # Default to text
                    with open(params.context_file_path, "r", encoding="utf-8") as f:
                        context = f.read()
            else:
                return f"Error: File not found at {params.context_file_path}"
        else:
            return "Error: No context provided (text or file)."

        # 2. Config
        api_key = os.getenv("DEEPSEEK_API_KEY")
        if not api_key:
            return "Error: DEEPSEEK_API_KEY required for RLM."
            
        provider = DeepSeekProvider(api_key)
        config = RLMConfig(max_iterations=params.max_iterations)
        orchestrator = RLMOrchestrator(config, provider)

        # 3. Run Async Process
        print(f"[{self.name}] Starting RLM on {len(context)} chars context...")
        
        # Inject instruction to look at context variable
        effective_query = (
            f"The analyzed document text is loaded into the Python variable `context` "
            f"(Length: {len(context)} chars). "
            f"DO NOT perform new file I/O. Use `context` directly.\n\n"
            f"User Request: {params.query}"
        )
        
        try:
            # Since we are now in async loop, just await directly
            result = await orchestrator.process(effective_query, context)
            return result
        except Exception as e:
            return f"RLM Execution Failed: {e}"
