from django.db import models


class CommentRater(models.Model):
    ip = models.CharField(max_length=39)
    comment = models.ForeignKey('Comment', on_delete=models.CASCADE, related_name='raters')

    class Meta:
        managed = False
        db_table = 'comment_raters'
        verbose_name = 'Comment Rater'
        verbose_name_plural = 'Comment Raters'

    def __str__(self):
        return f"{self.ip} on comment #{self.comment_id}"
