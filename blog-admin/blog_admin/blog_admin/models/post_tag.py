from django.db import models


class PostTag(models.Model):
    post = models.ForeignKey('Post', on_delete=models.CASCADE, db_column='post_id')
    tag = models.ForeignKey('Tag', on_delete=models.CASCADE, db_column='tag_id')
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        managed = False
        db_table = 'posts_tags'
        unique_together = [('post', 'tag')]
        verbose_name = 'Post Tag'
        verbose_name_plural = 'Post Tags'

    def __str__(self):
        return f"{self.post} - {self.tag}"
