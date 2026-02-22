from django.apps import apps
from django.test.runner import DiscoverRunner


class UnmanagedModelTestRunner(DiscoverRunner):
    """
    Test runner that creates DB tables for unmanaged models.

    All blog_admin models use managed=False because the schema is owned
    by Flyway (blog-api). During testing, we temporarily set managed=True
    so Django can create the tables in the test database.
    """

    def setup_databases(self, **kwargs):
        self._unmanaged_models = []
        for model in apps.get_app_config('blog_admin').get_models():
            if not model._meta.managed:
                model._meta.managed = True
                self._unmanaged_models.append(model)

        result = super().setup_databases(**kwargs)

        # Restore managed=False after tables are created so that tests
        # checking Meta.managed see the real value.
        for model in self._unmanaged_models:
            model._meta.managed = False

        return result
