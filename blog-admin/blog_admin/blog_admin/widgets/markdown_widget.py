from django.forms import Textarea

EASYMDE_VERSION = '2.21.0'
EASYMDE_CSS_SRI = 'sha384-ZoLYv3S+AsZX+zhbN1D1+WPpc8f+DmLfxfgw+qn0Nq8wJPOYQQXEW5ZrRhcGozlG'
EASYMDE_JS_SRI = 'sha384-mTM6vzy+/UiHrMBClNGViM9qEv0/26iCGqpJKhSzdnjrxbKjO3vkT62ujXQ8B5iv'


class MarkdownWidget(Textarea):
    """Textarea widget enhanced with EasyMDE markdown editor."""

    template_name = 'admin/widgets/markdown_widget.html'

    def get_context(self, name, value, attrs):
        context = super().get_context(name, value, attrs)
        context['easymde_version'] = EASYMDE_VERSION
        context['easymde_css_sri'] = EASYMDE_CSS_SRI
        context['easymde_js_sri'] = EASYMDE_JS_SRI
        return context

    def __init__(self, attrs=None):
        default_attrs = {'class': 'markdown-editor'}
        if attrs:
            default_attrs.update(attrs)
        super().__init__(attrs=default_attrs)
