from wpssite.config.models import Config

def meta(request):
    return {
        'META_TITLE': Config.params.get('meta_title', True),
        'META_KEYWORDS': Config.params.get('meta_keywords'),
        'META_DESCRIPTION': Config.params.get('meta_description')
    }