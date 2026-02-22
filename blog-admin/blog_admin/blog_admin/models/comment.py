from django.db import models


class Comment(models.Model):
    text = models.TextField()
    name = models.CharField(max_length=255)
    email = models.CharField(max_length=75)
    post = models.ForeignKey('Post', on_delete=models.CASCADE, related_name='comments')
    parent = models.ForeignKey(
        'self', on_delete=models.CASCADE, null=True, blank=True, related_name='replies'
    )
    rating = models.IntegerField(default=0)
    is_approved = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        managed = False
        db_table = 'comments'
        verbose_name_plural = 'Comments'
        ordering = ['-created_at']

    def __str__(self):
        return f"Comment by {self.name} on {self.post}"
