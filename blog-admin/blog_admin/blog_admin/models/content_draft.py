from django.db import models


class ContentDraft(models.Model):
    PLATFORM_CHOICES = [
        ('blog', 'Blog'),
        ('linkedin', 'LinkedIn'),
        ('twitter', 'Twitter/X'),
    ]
    STATUS_CHOICES = [
        ('idea', 'Idea'),
        ('generating', 'Generating'),
        ('draft', 'Draft'),
        ('review', 'Review'),
        ('approved', 'Approved'),
        ('published', 'Published'),
        ('archived', 'Archived'),
    ]

    platform = models.CharField(max_length=20, choices=PLATFORM_CHOICES)
    title = models.TextField(blank=True, null=True)
    body = models.TextField(default='')
    status = models.CharField(
        max_length=20, choices=STATUS_CHOICES, default='draft'
    )
    source_post = models.ForeignKey(
        'Post',
        null=True,
        blank=True,
        on_delete=models.SET_NULL,
        related_name='derived_drafts',
    )
    language = models.ForeignKey(
        'Language',
        on_delete=models.PROTECT,
        db_column='language_code',
        to_field='code',
        default='en',
    )
    metadata = models.JSONField(default=dict, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        managed = False
        db_table = 'content_drafts'
        ordering = ['-updated_at']

    def __str__(self):
        title = self.title or '(untitled)'
        return f"[{self.platform}] {title} ({self.status})"
