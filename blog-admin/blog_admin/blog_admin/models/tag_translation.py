from django.db import models


class TagTranslation(models.Model):
    tag = models.ForeignKey(
        'Tag', on_delete=models.CASCADE, related_name='translations'
    )
    language = models.ForeignKey(
        'Language',
        on_delete=models.CASCADE,
        db_column='language_code',
        to_field='code',
    )
    name = models.TextField()
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        managed = False
        db_table = 'tag_translations'
        unique_together = [('tag', 'language')]

    def __str__(self):
        return f"{self.name} [{self.language_id}]"
