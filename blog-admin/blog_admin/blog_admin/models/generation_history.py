from django.db import models


class GenerationHistory(models.Model):
    draft = models.ForeignKey(
        'ContentDraft', on_delete=models.CASCADE, related_name='generations'
    )
    prompt = models.TextField()
    model = models.CharField(max_length=100)
    response = models.TextField()
    token_usage = models.JSONField(null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        managed = False
        db_table = 'generation_history'
        ordering = ['-created_at']
        verbose_name_plural = 'generation history'

    def __str__(self):
        return f"Generation {self.pk} ({self.model}) for draft {self.draft_id}"

    @property
    def input_tokens(self):
        return (self.token_usage or {}).get('input', 0)

    @property
    def output_tokens(self):
        return (self.token_usage or {}).get('output', 0)
