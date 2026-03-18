from django.db import models


class Language(models.Model):
    code = models.CharField(max_length=5, primary_key=True)
    name = models.CharField(max_length=50)
    native_name = models.CharField(max_length=50)
    is_default = models.BooleanField(default=False)
    is_active = models.BooleanField(default=True)
    sort_order = models.IntegerField(default=0)

    class Meta:
        managed = False
        db_table = 'languages'
        ordering = ['sort_order']

    def __str__(self):
        return f"{self.native_name} ({self.code})"
