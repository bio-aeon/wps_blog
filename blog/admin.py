from django import forms
from django.contrib import admin

from ckeditor.widgets import CKEditorWidget

from .models import Post, Comment


class PostAdminForm(forms.ModelForm):
    text = forms.CharField(widget=CKEditorWidget())
    class Meta:
        model = Post


class PostAdmin(admin.ModelAdmin):
    form = PostAdminForm
    list_display = ('name', 'create_time', 'hidden')

admin.site.register(Post, PostAdmin)
admin.site.register(Comment)