from django.db import models


class Tag(models.Model):
    name = models.CharField(max_length=100)
    slug = models.SlugField(max_length=100, unique=True)

    class Meta:
        managed = False
        db_table = 'tags'
        verbose_name_plural = 'Tags'
        ordering = ['name']

    def __str__(self):
        return self.name
