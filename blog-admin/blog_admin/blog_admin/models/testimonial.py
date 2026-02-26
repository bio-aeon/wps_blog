from django.db import models


class Testimonial(models.Model):
    author_name = models.CharField(max_length=255)
    author_title = models.CharField(max_length=255, blank=True, null=True)
    author_company = models.CharField(max_length=255, blank=True, null=True)
    author_url = models.CharField(max_length=500, blank=True, null=True)
    author_image_url = models.CharField(max_length=500, blank=True, null=True)
    quote = models.TextField()
    sort_order = models.IntegerField(default=0)
    is_active = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        managed = False
        db_table = 'testimonials'
        ordering = ['sort_order']

    def __str__(self):
        company_info = f" ({self.author_company})" if self.author_company else ""
        return f"{self.author_name}{company_info}"
