from django import forms

from blog_admin.models import Post, Page
from blog_admin.widgets import MarkdownWidget


class PostAdminForm(forms.ModelForm):
    class Meta:
        model = Post
        fields = '__all__'
        widgets = {
            'text': MarkdownWidget(),
            'short_text': MarkdownWidget(attrs={'style': 'min-height: 150px;'}),
        }


class PageAdminForm(forms.ModelForm):
    class Meta:
        model = Page
        fields = '__all__'
        widgets = {
            'content': MarkdownWidget(),
        }
