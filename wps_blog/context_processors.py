from config.models import Config
import utils

def meta(request):
    return {
        'META_TITLE': Config.params.get('meta_title', True),
        'META_KEYWORDS': Config.params.get('meta_keywords'),
        'META_DESCRIPTION': Config.params.get('meta_description')
    }

def is_localhost(request):
    return {
        'IS_LOCALHOST': utils.get_client_ip(request) == '127.0.0.1'
    }