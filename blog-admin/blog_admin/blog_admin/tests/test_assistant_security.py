import os
from unittest.mock import patch

from django.test import TestCase, Client

from blog_admin.assistant.services.encryption import (
    encrypt_value,
    decrypt_value,
    get_llm_api_key,
)
from blog_admin.models import Config, User


class EncryptionTest(TestCase):

    def test_encrypt_decrypt_roundtrip(self):
        plaintext = 'sk-ant-api03-test-key-12345'
        ciphertext = encrypt_value(plaintext)
        self.assertNotEqual(ciphertext, plaintext)
        self.assertEqual(decrypt_value(ciphertext), plaintext)

    def test_decrypt_with_tampered_ciphertext_fails(self):
        from cryptography.fernet import InvalidToken
        ciphertext = encrypt_value('some-key')
        tampered = ciphertext[:-5] + 'XXXXX'
        with self.assertRaises(InvalidToken):
            decrypt_value(tampered)

    def test_encrypt_produces_different_ciphertexts(self):
        plaintext = 'same-key'
        c1 = encrypt_value(plaintext)
        c2 = encrypt_value(plaintext)
        self.assertNotEqual(c1, c2)


class GetLLMApiKeyTest(TestCase):

    def test_resolves_from_encrypted_config(self):
        encrypted = encrypt_value('sk-from-db')
        Config.objects.create(
            name='assistant_anthropic_api_key',
            value=encrypted,
            comment='test',
        )
        self.assertEqual(get_llm_api_key('anthropic'), 'sk-from-db')

    @patch.dict(os.environ, {'ANTHROPIC_API_KEY': 'sk-from-env'})
    def test_falls_back_to_env_var(self):
        self.assertEqual(get_llm_api_key('anthropic'), 'sk-from-env')

    @patch.dict(os.environ, {}, clear=True)
    def test_rejects_when_no_key_configured(self):
        from django.core.exceptions import ImproperlyConfigured
        with self.assertRaises(ImproperlyConfigured):
            get_llm_api_key('anthropic')

    def test_config_takes_priority_over_env(self):
        encrypted = encrypt_value('sk-from-db')
        Config.objects.create(
            name='assistant_anthropic_api_key',
            value=encrypted,
            comment='test',
        )
        with patch.dict(os.environ, {'ANTHROPIC_API_KEY': 'sk-from-env'}):
            self.assertEqual(get_llm_api_key('anthropic'), 'sk-from-db')


class AssistantAuthRequiredTest(TestCase):

    def test_dashboard_redirects_anonymous(self):
        response = self.client.get('/admin/assistant/')
        self.assertIn(response.status_code, [301, 302])

    def test_suggest_redirects_anonymous(self):
        response = self.client.post('/admin/assistant/suggest/blog/')
        self.assertIn(response.status_code, [301, 302])

    def test_generate_redirects_anonymous(self):
        response = self.client.post('/admin/assistant/generate/blog/')
        self.assertIn(response.status_code, [301, 302])

    def test_drafts_redirects_anonymous(self):
        response = self.client.get('/admin/assistant/drafts/')
        self.assertIn(response.status_code, [301, 302])


class AssistantCsrfProtectionTest(TestCase):

    def setUp(self):
        self.admin_user = User.objects.create_superuser(
            'admin', 'a@b.com', 'testpass123'
        )

    def test_rejects_post_without_csrf_token(self):
        client = Client(enforce_csrf_checks=True)
        client.force_login(self.admin_user)
        response = client.post(
            '/admin/assistant/drafts/',
            data='{"platform": "blog", "body": "test"}',
            content_type='application/json',
        )
        self.assertEqual(response.status_code, 403)
