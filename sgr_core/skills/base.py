from abc import ABC, abstractmethod
from typing import Type, Any, Dict
from pydantic import BaseModel
from core.state import AgentState

from core.state import AgentState

class BaseSkill(ABC):
    """
    Abstract Base Class for all capabilities (Skills).
    Each skill represents a specialized domain (formerly a separate agent).
    """
    
    def set_rag(self, rag_service: Any):
        """Optional: Inject RAG Service if the skill needs it."""
        self.rag = rag_service
    
    @property
    @abstractmethod
    def name(self) -> str:
        """The unique identifier for this skill (e.g., 'portfolio_manager')"""
        pass

    @property
    @abstractmethod
    def description(self) -> str:
        """A natural language description of what this skill does for the Router/Planner."""
        pass

    @property
    @abstractmethod
    def input_schema(self) -> Type[BaseModel]:
        """The Pydantic model defining the strict input structure this skill requires."""
        pass
    
    def is_sensitive(self, params: BaseModel) -> bool:
        """
        Return True if this action requires human approval.
        Override this in specific skills (e.g. for file deletion/writing).
        """
        return False

    @abstractmethod
    async def execute(self, params: BaseModel, state: 'AgentState') -> str:
        """
        Execute the skill logic asynchronously.
        
        Args:
            params: The validated instance of input_schema.
            state: The global agent state.
            
        Returns:
            A string summary of the result to be added to the conversation history.
        """
        pass
