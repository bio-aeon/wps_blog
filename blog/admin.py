from django.contrib import admin
from .models import Post, Comment


class PostAdmin(admin.ModelAdmin):
	list_display = ('name', 'create_time', 'hidden')

admin.site.register(Post, PostAdmin)
admin.site.register(Comment)