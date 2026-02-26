from django.db import models


class Experience(models.Model):
    company = models.CharField(max_length=255)
    position = models.CharField(max_length=255)
    description = models.TextField()
    start_date = models.DateField()
    end_date = models.DateField(blank=True, null=True)
    location = models.CharField(max_length=255, blank=True, null=True)
    company_url = models.CharField(max_length=500, blank=True, null=True)
    sort_order = models.IntegerField(default=0)
    is_active = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        managed = False
        db_table = 'experiences'
        ordering = ['sort_order', '-start_date']

    def __str__(self):
        end = self.end_date or "Present"
        return f"{self.position} at {self.company} ({self.start_date} – {end})"
