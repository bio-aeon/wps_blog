from django.db import models


class Skill(models.Model):
    name = models.CharField(max_length=100)
    slug = models.SlugField(max_length=100, unique=True)
    category = models.CharField(max_length=100)
    proficiency = models.IntegerField(default=0)
    icon = models.CharField(max_length=255, blank=True, null=True)
    sort_order = models.IntegerField(default=0)
    is_active = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        managed = False
        db_table = 'skills'
        ordering = ['sort_order', 'name']

    def __str__(self):
        return f"{self.name} ({self.category})"
