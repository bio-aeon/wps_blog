from django import forms

from blog_admin.models import Post, Page


class PostAdminForm(forms.ModelForm):
    class Meta:
        model = Post
        fields = '__all__'


class PageAdminForm(forms.ModelForm):
    class Meta:
        model = Page
        fields = '__all__'
