import base64
import hashlib
import os

from cryptography.fernet import Fernet, InvalidToken
from django.conf import settings
from django.core.exceptions import ImproperlyConfigured


def _get_encryption_key():
    """Derive a Fernet key from Django's SECRET_KEY."""
    key = hashlib.sha256(settings.SECRET_KEY.encode()).digest()
    return base64.urlsafe_b64encode(key)


def encrypt_value(plaintext):
    """Encrypt a plaintext string using Fernet symmetric encryption."""
    f = Fernet(_get_encryption_key())
    return f.encrypt(plaintext.encode()).decode()


def decrypt_value(ciphertext):
    """Decrypt a Fernet-encrypted string. Raises InvalidToken on failure."""
    f = Fernet(_get_encryption_key())
    return f.decrypt(ciphertext.encode()).decode()


def get_llm_api_key(provider='anthropic'):
    """
    Retrieve LLM API key with fallback chain:
    1. Encrypted value in configs table (e.g., assistant_anthropic_api_key)
    2. Environment variable (e.g., ANTHROPIC_API_KEY)
    """
    from blog_admin.models import Config

    config_name = f'assistant_{provider}_api_key'
    env_var = f'{provider.upper()}_API_KEY'

    try:
        config = Config.objects.filter(name=config_name).first()
        if config and config.value:
            try:
                return decrypt_value(config.value)
            except InvalidToken:
                pass
    except Exception:
        pass

    api_key = os.environ.get(env_var, '')
    if api_key:
        return api_key

    raise ImproperlyConfigured(
        f"No API key found for '{provider}'. Set the '{env_var}' environment "
        f"variable or store an encrypted key in configs as '{config_name}'."
    )
