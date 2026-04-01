import json
import functools

from django.contrib.admin.views.decorators import staff_member_required
from django.http import JsonResponse


def admin_json_view(view_func):
    """
    Decorator for admin-only JSON endpoints.

    - Requires staff authentication (redirects to login otherwise)
    - Parses JSON request body for POST/PUT/PATCH into request.json_data
    - Returns 400 on malformed JSON
    - Returns 405 for unsupported HTTP methods
    """

    @staff_member_required
    @functools.wraps(view_func)
    def wrapper(request, *args, **kwargs):
        if request.method in ('POST', 'PUT', 'PATCH'):
            if request.body:
                try:
                    request.json_data = json.loads(request.body)
                except (json.JSONDecodeError, ValueError):
                    return JsonResponse(
                        {'error': 'Invalid JSON in request body'}, status=400
                    )
            else:
                request.json_data = {}
        else:
            request.json_data = {}

        return view_func(request, *args, **kwargs)

    return wrapper
