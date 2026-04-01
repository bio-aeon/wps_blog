from django.db import models

from blog_admin.models.content_draft import ContentDraft


class ContentTemplate(models.Model):
    name = models.CharField(max_length=255)
    platform = models.CharField(
        max_length=20, choices=ContentDraft.PLATFORM_CHOICES
    )
    prompt_template = models.TextField()
    description = models.TextField(blank=True, null=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        managed = False
        db_table = 'content_templates'
        ordering = ['platform', 'name']

    def __str__(self):
        return f"[{self.platform}] {self.name}"
