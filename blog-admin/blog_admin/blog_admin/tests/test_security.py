from django.conf import settings
from django.test import TestCase, Client

from blog_admin.models import User


class SecretKeySecurityTest(TestCase):

    def test_secret_key_is_not_insecure_default(self):
        self.assertFalse(
            settings.SECRET_KEY.startswith('django-insecure-'),
            'Test environment must not use an insecure SECRET_KEY',
        )

    def test_secret_key_is_not_empty(self):
        self.assertTrue(len(settings.SECRET_KEY) > 0)


class SecurityMiddlewareTest(TestCase):

    def test_security_middleware_is_installed(self):
        self.assertIn(
            'django.middleware.security.SecurityMiddleware',
            settings.MIDDLEWARE,
        )

    def test_csrf_middleware_is_installed(self):
        self.assertIn(
            'django.middleware.csrf.CsrfViewMiddleware',
            settings.MIDDLEWARE,
        )

    def test_clickjacking_middleware_is_installed(self):
        self.assertIn(
            'django.middleware.clickjacking.XFrameOptionsMiddleware',
            settings.MIDDLEWARE,
        )

    def test_password_validators_configured(self):
        self.assertTrue(len(settings.AUTH_PASSWORD_VALIDATORS) >= 4)


class CsrfProtectionTest(TestCase):

    def setUp(self):
        self.admin_user = User.objects.create_superuser(
            'admin', 'a@b.com', 'testpass123'
        )

    def test_csrf_enforced_on_login(self):
        client = Client(enforce_csrf_checks=True)
        response = client.post('/admin/login/', {
            'username': 'admin',
            'password': 'testpass123',
        })
        self.assertEqual(response.status_code, 403)


class StaffRequiredTest(TestCase):

    def test_dashboard_requires_authentication(self):
        response = self.client.get('/admin/dashboard/')
        self.assertIn(response.status_code, [301, 302])

    def test_analytics_requires_authentication(self):
        response = self.client.get('/admin/analytics/')
        self.assertIn(response.status_code, [301, 302])


class ProductionSecuritySettingsTest(TestCase):

    def test_production_settings_module_defines_hsts(self):
        import inspect
        import blog_admin.settings as settings_module
        source = inspect.getsource(settings_module)
        self.assertIn('SECURE_HSTS_SECONDS', source)
        self.assertIn('SESSION_COOKIE_SECURE', source)
        self.assertIn('CSRF_COOKIE_SECURE', source)
        self.assertIn('SECURE_SSL_REDIRECT', source)
        self.assertIn('SECURE_PROXY_SSL_HEADER', source)

    def test_production_guard_uses_debug_flag(self):
        import inspect
        import blog_admin.settings as settings_module
        source = inspect.getsource(settings_module)
        self.assertIn('if not DEBUG:', source)

    def test_hsts_seconds_set_in_production_block(self):
        self.assertTrue(
            hasattr(settings, 'SECURE_HSTS_SECONDS'),
            'SECURE_HSTS_SECONDS should be defined',
        )
        self.assertEqual(settings.SECURE_HSTS_SECONDS, 31536000)

    def test_session_cookie_secure_in_production(self):
        self.assertTrue(settings.SESSION_COOKIE_SECURE)

    def test_csrf_cookie_secure_in_production(self):
        self.assertTrue(settings.CSRF_COOKIE_SECURE)
