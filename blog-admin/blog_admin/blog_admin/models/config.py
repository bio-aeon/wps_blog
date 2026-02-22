from django.db import models


class Config(models.Model):
    name = models.CharField(max_length=255, unique=True)
    value = models.CharField(max_length=255)
    comment = models.TextField()
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        managed = False
        db_table = 'configs'
        verbose_name_plural = 'Configs'
        ordering = ['name']

    def __str__(self):
        return f"{self.name} = {self.value}"
