from django.db import models


class SocialLink(models.Model):
    platform = models.CharField(max_length=50)
    url = models.CharField(max_length=500)
    label = models.CharField(max_length=100, blank=True, null=True)
    icon = models.CharField(max_length=255, blank=True, null=True)
    sort_order = models.IntegerField(default=0)
    is_active = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        managed = False
        db_table = 'social_links'
        ordering = ['sort_order']

    def __str__(self):
        return f"{self.platform}: {self.label or self.url}"
