from django.db import models


class Page(models.Model):
    url = models.CharField(max_length=100, unique=True)
    title = models.CharField(max_length=255)
    content = models.TextField()
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        managed = False
        db_table = 'pages'
        verbose_name_plural = 'Pages'
        ordering = ['title']

    def __str__(self):
        return self.title
