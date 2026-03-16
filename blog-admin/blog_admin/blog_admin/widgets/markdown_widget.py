from django.forms import Textarea

EASYMDE_CSS_SRI = 'sha384-uqD/OYCNfagd1EgXMgl5QedTD5K+B3e9b8GYo/41t7+Serf7CBxvl+tU1gHd+qd1'
EASYMDE_JS_SRI = 'sha384-KtB38COewxfrhJxoN2d+olxJAeT08LF8cVZ6DQ8Poqu89zIptqO6zAXoIxpGNWYE'


class MarkdownWidget(Textarea):
    """Textarea widget enhanced with EasyMDE markdown editor."""

    template_name = 'admin/widgets/markdown_widget.html'

    def get_context(self, name, value, attrs):
        context = super().get_context(name, value, attrs)
        context['easymde_css_sri'] = EASYMDE_CSS_SRI
        context['easymde_js_sri'] = EASYMDE_JS_SRI
        return context

    def __init__(self, attrs=None):
        default_attrs = {'class': 'markdown-editor'}
        if attrs:
            default_attrs.update(attrs)
        super().__init__(attrs=default_attrs)
