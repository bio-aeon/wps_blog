from django.forms import Textarea


class MarkdownWidget(Textarea):
    """Textarea widget enhanced with EasyMDE markdown editor."""

    template_name = 'admin/widgets/markdown_widget.html'

    class Media:
        css = {
            'all': (
                'https://cdn.jsdelivr.net/npm/easymde@2.18.0/dist/easymde.min.css',
            )
        }
        js = (
            'https://cdn.jsdelivr.net/npm/easymde@2.18.0/dist/easymde.min.js',
        )

    def __init__(self, attrs=None):
        default_attrs = {'class': 'markdown-editor'}
        if attrs:
            default_attrs.update(attrs)
        super().__init__(attrs=default_attrs)
