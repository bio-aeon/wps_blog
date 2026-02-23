from django.test import TestCase

from blog_admin.forms import PostAdminForm, PageAdminForm
from blog_admin.widgets import MarkdownWidget


class PostAdminFormTest(TestCase):
    def test_text_field_uses_markdown_widget(self):
        form = PostAdminForm()
        self.assertIsInstance(form.fields['text'].widget, MarkdownWidget)

    def test_short_text_field_uses_markdown_widget(self):
        form = PostAdminForm()
        self.assertIsInstance(form.fields['short_text'].widget, MarkdownWidget)


class PageAdminFormTest(TestCase):
    def test_content_field_uses_markdown_widget(self):
        form = PageAdminForm()
        self.assertIsInstance(form.fields['content'].widget, MarkdownWidget)
