from django.db import models


class Post(models.Model):
    name = models.CharField(max_length=255)
    short_text = models.CharField(max_length=255)
    text = models.TextField()
    author = models.ForeignKey('User', on_delete=models.DO_NOTHING)
    views = models.IntegerField(default=0)
    meta_title = models.CharField(max_length=255)
    meta_keywords = models.CharField(max_length=255)
    meta_description = models.CharField(max_length=255)
    is_hidden = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now=False, auto_now_add=True)

    class Meta:
        managed = False
        db_table = 'posts'
        verbose_name_plural = 'Posts'
        ordering = ['-created_at']

    def __str__(self):
        return self.name
