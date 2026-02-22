"""Settings overrides for running tests.

Uses SQLite so tests can run without a PostgreSQL server.
Usage: python manage.py test --settings=blog_admin.test_settings
"""

from blog_admin.settings import *  # noqa: F401, F403

DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.sqlite3',
        'NAME': ':memory:',
    }
}
