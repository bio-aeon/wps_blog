from django.test import TestCase

from blog_admin.widgets import MarkdownWidget


class MarkdownWidgetTest(TestCase):
    def test_widget_has_css_class(self):
        widget = MarkdownWidget()
        self.assertIn('markdown-editor', widget.attrs.get('class', ''))

    def test_widget_media_includes_easymde_css(self):
        widget = MarkdownWidget()
        css = str(widget.media)
        self.assertIn('easymde.min.css', css)

    def test_widget_media_includes_easymde_js(self):
        widget = MarkdownWidget()
        js = str(widget.media)
        self.assertIn('easymde.min.js', js)

    def test_widget_renders_textarea(self):
        widget = MarkdownWidget()
        html = widget.render('content', 'Hello **world**', attrs={'id': 'id_content'})
        self.assertIn('<textarea', html)
        self.assertIn('Hello **world**', html)
        self.assertIn('id_content', html)

    def test_widget_renders_easymde_init_script(self):
        widget = MarkdownWidget()
        html = widget.render('content', '', attrs={'id': 'id_content'})
        self.assertIn('new EasyMDE', html)
        self.assertIn('id_content', html)

    def test_custom_attrs_merged(self):
        widget = MarkdownWidget(attrs={'style': 'min-height: 150px;'})
        self.assertIn('markdown-editor', widget.attrs.get('class', ''))
        self.assertEqual(widget.attrs.get('style'), 'min-height: 150px;')
