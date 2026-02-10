import os
from typing import Dict, Type, List, Optional
from pydantic import BaseModel, Field

from core.state import AgentState
from core.llm import LLMService
from skills.base import BaseSkill
from core.logger import get_logger

from datetime import datetime

logger = get_logger("core.engine")

class RouterDecision(BaseModel):
    """Schema for the Router's decision."""
    reasoning: str = Field(description="Step-by-step reasoning for why this skill was chosen.")
    selected_skill_name: Optional[str] = Field(description="The exact name of the skill to use, or null if no skill is needed (chitchat).")
    direct_response: Optional[str] = Field(description="If no skill is needed, provide the response here.")

from core.rag import CoreRAGService

from core.memory import PersistentMemory

from typing import Dict, Type, List, Optional, Callable, Awaitable

from core.security import SecurityGuardian, SecurityViolationError

class CoreEngine:
    def __init__(self, llm_config: Dict[str, str] = None, user_id: str = "default_user", approval_callback: Callable[[str], Awaitable[bool]] = None):
        logger.info("Initializing CoreEngine...", user_id=user_id)
        self.user_id = user_id
        self.approval_callback = approval_callback
        self.memory = PersistentMemory()
        
        # Load state with persistent history
        self.state = AgentState(user_request="")
        self._load_context()
        
        self.llm = LLMService(**(llm_config or {}))
        
        # Initialize Global RAG with env vars
        qdrant_host = os.getenv("QDRANT_HOST", "localhost")
        qdrant_port = int(os.getenv("QDRANT_PORT", 6333))
        ollama_url = os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
        
        self.rag = CoreRAGService(ollama_base_url=ollama_url, qdrant_host=qdrant_host, qdrant_port=qdrant_port)
        self.skills: Dict[str, BaseSkill] = {}

        # Security
        self.security = SecurityGuardian()

    def _load_context(self):
        """Load history and profile from DB."""
        # Load history
        msgs = self.memory.get_history(self.user_id, limit=20)
        self.state.history = msgs
        
        # Load preferences into context
        prefs = self.memory.get_user_preferences(self.user_id)
        self.state.current_context.update(prefs)

    def _save_message(self, role: str, content: str):
        """Save to DB and State."""
        self.memory.add_message(self.user_id, role, content)
        self.state.add_message(role, content)

    def register_skill(self, skill: BaseSkill):
        # Inject RAG service into skill if it supports it
        if hasattr(skill, 'set_rag'):
            skill.set_rag(self.rag)
            
        self.skills[skill.name] = skill
        logger.info("Registered skill", skill=skill.name)

    async def run(self, user_input: str) -> str:
        """
        Main SGR Loop (Async).
        """
        # Bind request-specific context to logger
        log = logger.bind(request_len=len(user_input))
        log.info("Processing user request", query=user_input[:50]+"...")
        
        try:
            # 0. SECURITY CHECK (Input Phase)
            self.security.validate(user_input)
        except SecurityViolationError as e:
            log.warning("Security violation detected", error=str(e))
            return str(e)  # Return the alert message directly to user

        # 1. Update State
        self.state.user_request = user_input
        self._save_message("user", user_input)
        
        # 2. Routing / Planning (SGR Phase 1)
        decision = await self._route_request(user_input)
        log.info("Routing complete", skill=decision.selected_skill_name, reasoning=decision.reasoning)
        
        if not decision.selected_skill_name:
            # Handle as direct conversation
            response = decision.direct_response or "I'm not sure how to help with that, and no specific skill matched."
            self._save_message("assistant", response)
            log.info("Handled as direct chat")
            return response

        skill_name = decision.selected_skill_name
        if skill_name not in self.skills:
             log.error("Unknown skill selected", skill=skill_name)
             return f"Error: Router selected unknown skill '{skill_name}'"

        skill = self.skills[skill_name]
        self.state.active_skill_name = skill_name
        
        # 3. Schema Generation (SGR Phase 2)
        log.info("Generating schema", skill=skill_name)
        try:
            now_str = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            history_text = "\n".join([f"{m.role}: {m.content}" for m in self.state.history[-5:]])
            
            skill_input = await self.llm.generate_structured(
                system_prompt=f"You are an expert at {skill_name}. {skill.description}\nCurrent Time: {now_str}",
                user_prompt=f"Context:\n{history_text}\n\nUser Request: {user_input}\n\nIdentify the parameters needed.",
                response_model=skill.input_schema
            )
        except Exception as e:
            log.error("Schema generation failed", error=str(e), skill=skill_name)
            return f"Failed to generate valid plan for {skill_name}: {e}"

        # 4. Execution (Action Phase)
        # Check for Sensitivity / Human-in-the-Loop
        if getattr(skill, 'is_sensitive', lambda x: False)(skill_input):
            decision_msg = f"Skill '{skill_name}' wants to execute sensitive action:\n{skill_input}"
            log.info("Requesting human approval", reason="sensitive_action")
            
            is_approved = False
            if self.approval_callback:
                try:
                    is_approved = await self.approval_callback(decision_msg)
                except Exception as e:
                    log.error("Approval callback failed", error=str(e))
            
            if not is_approved:
                log.warning("Action denied by user", skill=skill_name)
                return "Operation cancelled by user (Human-in-the-Loop)."

        log.info("Executing skill", skill=skill_name, params=str(skill_input))
        try:
            result = await skill.execute(skill_input, self.state)
            self._save_message("assistant", result)
            log.info("Skill execution success", result_len=len(result))
            return result
        except Exception as e:
            log.error("Skill execution failed", error=str(e), skill=skill_name)
            return f"Error executing skill {skill_name}: {e}"

    async def _route_request(self, text: str) -> RouterDecision:
        skills_desc = "\n".join([f"- {s.name}: {s.description}" for s in self.skills.values()])
        
        # Include a bit of history in routing to handle follow-ups
        # Include more history in routing to handle follow-ups and repeats
        history_snippet = "\n".join([f"{m.role}: {m.content[:4000]}..." if len(m.content) > 4000 else f"{m.role}: {m.content}" for m in self.state.history[-5:]])
        logger.info("Router History Context", snippet=history_snippet)
        
        system_prompt = (
            "You are Buratino (Буратино), a smart, cheerful, and helpful AI assistant. Your job is to route the user's request to the correct Skill.\n"
            f"Available Skills:\n{skills_desc}\n\n"
            "Analyze the user's request and context. If it matches a skill, select it.\n"
            "CRITICAL:\n"
            "- If the user asks to PERFORM an action (create, generate, save, search), SELECT THE SKILL even if it was done previously.\n"
            "- If the user asks a QUESTION that is already answered in History, select null (answer directly)."
        )
        
        return await self.llm.generate_structured(
            system_prompt=system_prompt,
            user_prompt=f"Context:\n{history_snippet}\n\nCurrent Input: {text}",
            response_model=RouterDecision
        )
