from django.db import models


class PageTranslation(models.Model):
    TRANSLATION_STATUS_CHOICES = [
        ('draft', 'Draft'),
        ('in_progress', 'In Progress'),
        ('ready', 'Ready'),
        ('published', 'Published'),
    ]

    page = models.ForeignKey(
        'Page', on_delete=models.CASCADE, related_name='translations'
    )
    language = models.ForeignKey(
        'Language',
        on_delete=models.CASCADE,
        db_column='language_code',
        to_field='code',
    )
    title = models.TextField()
    content = models.TextField(blank=True, null=True)
    seo_title = models.CharField(max_length=255, blank=True, null=True)
    seo_description = models.TextField(blank=True, null=True)
    translation_status = models.CharField(
        max_length=20, choices=TRANSLATION_STATUS_CHOICES, default='draft'
    )
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        managed = False
        db_table = 'page_translations'
        unique_together = [('page', 'language')]

    def __str__(self):
        return f"{self.title} [{self.language_id}]"
