import json
from abc import ABC, abstractmethod
from dataclasses import dataclass

import anthropic

from blog_admin.assistant.services.encryption import get_llm_api_key
from blog_admin.models import Config


@dataclass
class LLMResponse:
    content: str
    model: str
    input_tokens: int
    output_tokens: int


class LLMClient(ABC):

    @abstractmethod
    def generate(self, system_prompt, user_prompt,
                 max_tokens=4096, temperature=0.7):
        """Generate text and return an LLMResponse."""

    def generate_json(self, system_prompt, user_prompt, max_tokens=4096):
        """Generate and parse a JSON response.

        Appends a JSON instruction to the system prompt.
        Returns (parsed_dict, LLMResponse).
        Raises ValueError if the response is not valid JSON.
        """
        json_system = (
            system_prompt
            + "\n\nIMPORTANT: Respond ONLY with valid JSON. "
            "No markdown fences, no commentary."
        )
        response = self.generate(
            json_system, user_prompt,
            max_tokens=max_tokens, temperature=0.4,
        )
        text = response.content.strip()
        if text.startswith('```'):
            text = text.split('\n', 1)[-1].rsplit('```', 1)[0].strip()
        try:
            parsed = json.loads(text)
        except json.JSONDecodeError as exc:
            raise ValueError(
                f"LLM returned invalid JSON: {exc}"
            ) from exc
        return parsed, response


class ClaudeClient(LLMClient):

    def __init__(self, api_key, model='claude-sonnet-4-6'):
        self.client = anthropic.Anthropic(api_key=api_key)
        self.model = model

    def generate(self, system_prompt, user_prompt,
                 max_tokens=4096, temperature=0.7):
        response = self.client.messages.create(
            model=self.model,
            system=system_prompt,
            messages=[{'role': 'user', 'content': user_prompt}],
            max_tokens=max_tokens,
            temperature=temperature,
        )
        return LLMResponse(
            content=response.content[0].text,
            model=self.model,
            input_tokens=response.usage.input_tokens,
            output_tokens=response.usage.output_tokens,
        )


def _get_model_for_platform(platform):
    """Read per-platform model from configs table, with a sensible default."""
    default_model = 'claude-sonnet-4-6'
    config_name = f'assistant_model_{platform}'
    try:
        config = Config.objects.filter(name=config_name).first()
        if config and config.value:
            return config.value
    except Exception:
        pass
    return default_model


def get_llm_client(platform='blog'):
    """Factory: return a configured LLM client for the given platform."""
    api_key = get_llm_api_key('anthropic')
    model = _get_model_for_platform(platform)
    return ClaudeClient(api_key=api_key, model=model)
