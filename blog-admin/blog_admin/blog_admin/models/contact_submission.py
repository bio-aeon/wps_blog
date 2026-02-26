from django.db import models


class ContactSubmission(models.Model):
    name = models.CharField(max_length=255)
    email = models.CharField(max_length=255)
    subject = models.CharField(max_length=500)
    message = models.TextField()
    ip_address = models.CharField(max_length=39, blank=True, null=True)
    is_read = models.BooleanField(default=False)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        managed = False
        db_table = 'contact_submissions'
        ordering = ['-created_at']

    def __str__(self):
        return f"{self.subject} (from {self.name})"
