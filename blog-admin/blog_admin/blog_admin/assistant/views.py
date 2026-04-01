from datetime import timedelta

from django.contrib.admin.views.decorators import staff_member_required
from django.db.models import Sum
from django.http import JsonResponse
from django.shortcuts import render
from django.utils import timezone
from django.views.decorators.http import require_http_methods

from blog_admin.assistant.constants import VALID_PLATFORMS
from blog_admin.assistant.decorators import admin_json_view
from blog_admin.assistant.services import draft as draft_service
from blog_admin.assistant.services import suggestion as suggestion_service
from blog_admin.assistant.services.generation import (
    generate_content as gen_content, RateLimitExceeded,
)
from blog_admin.assistant.services import refinement as refinement_service
from blog_admin.assistant.services import repurpose as repurpose_service
from blog_admin.models import Config, ContentDraft, ContentTemplate, GenerationHistory


DEFAULT_INPUT_TOKEN_COST = 0.000003
DEFAULT_OUTPUT_TOKEN_COST = 0.000015
DEFAULT_MONTHLY_BUDGET = 1_000_000
DEFAULT_ALERT_THRESHOLD = 0.8


def _get_config_float(name, default):
    try:
        config = Config.objects.filter(name=name).first()
        if config and config.value:
            return float(config.value)
    except (ValueError, Exception):
        pass
    return default


# -- Draft endpoints -----------------------------------------------------------

@admin_json_view
@require_http_methods(['GET', 'POST'])
def draft_list_create(request):
    if request.method == 'GET':
        platform = request.GET.get('platform')
        status = request.GET.get('status')
        limit = int(request.GET.get('limit', 20))
        offset = int(request.GET.get('offset', 0))
        result = draft_service.list_drafts(
            platform=platform, status=status, limit=limit, offset=offset,
        )
        return JsonResponse({
            'drafts': [draft_service.draft_to_dict(d) for d in result['drafts']],
            'total': result['total'],
        })

    data = request.json_data
    platform = data.get('platform')
    if not platform or platform not in draft_service.VALID_PLATFORMS:
        return JsonResponse(
            {'error': f"'platform' is required and must be one of: "
                      f"{', '.join(sorted(draft_service.VALID_PLATFORMS))}"},
            status=400,
        )
    body = data.get('body', '')
    draft = draft_service.create_draft(
        platform=platform,
        body=body,
        title=data.get('title'),
        language_code=data.get('language_code', 'en'),
        status=data.get('status', 'draft'),
        metadata=data.get('metadata'),
        source_post_id=data.get('source_post_id'),
    )
    return JsonResponse(draft_service.draft_to_dict(draft), status=201)


@admin_json_view
@require_http_methods(['GET', 'PUT', 'DELETE'])
def draft_detail(request, pk):
    if request.method == 'GET':
        draft = draft_service.get_draft(pk)
        return JsonResponse(draft_service.draft_to_dict(draft))

    if request.method == 'PUT':
        draft = draft_service.update_draft(pk, **request.json_data)
        return JsonResponse(draft_service.draft_to_dict(draft))

    draft_service.delete_draft(pk)
    return JsonResponse({}, status=204)


@admin_json_view
@require_http_methods(['POST'])
def draft_publish(request, pk):
    try:
        post = draft_service.publish_to_blog(pk, author=request.user)
    except ValueError as exc:
        return JsonResponse({'error': str(exc)}, status=400)
    return JsonResponse({
        'post_id': post.pk,
        'post_name': post.name,
        'message': 'Draft published as blog post.',
    })


# -- Suggestion endpoints (placeholder for Step 6) ----------------------------

@admin_json_view
@require_http_methods(['POST'])
def suggest_topics(request, platform):
    if platform not in VALID_PLATFORMS:
        return JsonResponse(
            {'error': f"Invalid platform '{platform}'. "
                      f"Must be one of: {', '.join(sorted(VALID_PLATFORMS))}"},
            status=400,
        )
    data = request.json_data
    try:
        result = suggestion_service.suggest_topics(
            platform=platform,
            count=data.get('count', 5),
            language=data.get('language', 'en'),
            focus_areas=data.get('focus_areas'),
            avoid_topics=data.get('avoid_topics'),
        )
    except ValueError as exc:
        return JsonResponse({'error': str(exc)}, status=502)
    return JsonResponse(result)


# -- Generation endpoints (placeholder for Step 7) ----------------------------

@admin_json_view
@require_http_methods(['POST'])
def generate_content(request, platform):
    if platform not in VALID_PLATFORMS:
        return JsonResponse(
            {'error': f"Invalid platform '{platform}'. "
                      f"Must be one of: {', '.join(sorted(VALID_PLATFORMS))}"},
            status=400,
        )
    data = request.json_data
    if not data.get('topic'):
        return JsonResponse({'error': "'topic' is required."}, status=400)
    try:
        result = gen_content(platform, data)
    except RateLimitExceeded as exc:
        return JsonResponse({'error': str(exc)}, status=429)
    except ValueError as exc:
        return JsonResponse({'error': str(exc)}, status=400)
    return JsonResponse(result, status=201)


@admin_json_view
@require_http_methods(['POST'])
def refine_content(request):
    data = request.json_data
    draft_id = data.get('draft_id')
    instructions = data.get('instructions')
    if not draft_id or not instructions:
        return JsonResponse(
            {'error': "'draft_id' and 'instructions' are required."}, status=400,
        )
    try:
        result = refinement_service.refine_content(draft_id, instructions)
    except RateLimitExceeded as exc:
        return JsonResponse({'error': str(exc)}, status=429)
    return JsonResponse(result)


@admin_json_view
@require_http_methods(['POST'])
def adjust_tone(request):
    data = request.json_data
    draft_id = data.get('draft_id')
    target_tone = data.get('target_tone')
    if not draft_id or not target_tone:
        return JsonResponse(
            {'error': "'draft_id' and 'target_tone' are required."}, status=400,
        )
    try:
        result = refinement_service.adjust_tone(draft_id, target_tone)
    except RateLimitExceeded as exc:
        return JsonResponse({'error': str(exc)}, status=429)
    except ValueError as exc:
        return JsonResponse({'error': str(exc)}, status=400)
    return JsonResponse(result)


@admin_json_view
@require_http_methods(['POST'])
def seo_suggest(request):
    data = request.json_data
    draft_id = data.get('draft_id')
    if not draft_id:
        return JsonResponse(
            {'error': "'draft_id' is required."}, status=400,
        )
    try:
        result = refinement_service.seo_suggest(draft_id)
    except RateLimitExceeded as exc:
        return JsonResponse({'error': str(exc)}, status=429)
    except ValueError as exc:
        return JsonResponse({'error': str(exc)}, status=400)
    return JsonResponse(result)


@admin_json_view
@require_http_methods(['POST'])
def repurpose_content(request):
    data = request.json_data
    source_post_id = data.get('source_post_id')
    target_platform = data.get('target_platform')
    if not source_post_id or not target_platform:
        return JsonResponse(
            {'error': "'source_post_id' and 'target_platform' are required."},
            status=400,
        )
    try:
        result = repurpose_service.repurpose_content(
            source_post_id=source_post_id,
            target_platform=target_platform,
            language=data.get('language', 'en'),
            format=data.get('format', 'single'),
            instructions=data.get('instructions', ''),
        )
    except RateLimitExceeded as exc:
        return JsonResponse({'error': str(exc)}, status=429)
    except ValueError as exc:
        return JsonResponse({'error': str(exc)}, status=400)
    return JsonResponse(result, status=201)


# -- Template endpoints (placeholder for Step 9) ------------------------------

def _template_to_dict(t):
    return {
        'id': t.pk,
        'name': t.name,
        'platform': t.platform,
        'prompt_template': t.prompt_template,
        'description': t.description,
        'created_at': t.created_at.isoformat() if t.created_at else None,
    }


@admin_json_view
@require_http_methods(['GET', 'POST'])
def template_list_create(request):
    if request.method == 'GET':
        platform = request.GET.get('platform')
        qs = ContentTemplate.objects.all()
        if platform:
            qs = qs.filter(platform=platform)
        return JsonResponse({
            'templates': [_template_to_dict(t) for t in qs],
            'total': qs.count(),
        })

    data = request.json_data
    name = data.get('name')
    platform = data.get('platform')
    prompt_template = data.get('prompt_template')
    if not all([name, platform, prompt_template]):
        return JsonResponse(
            {'error': "'name', 'platform', and 'prompt_template' are required."},
            status=400,
        )
    if platform not in VALID_PLATFORMS:
        return JsonResponse(
            {'error': f"Invalid platform. Must be one of: "
                      f"{', '.join(sorted(VALID_PLATFORMS))}"},
            status=400,
        )
    t = ContentTemplate.objects.create(
        name=name,
        platform=platform,
        prompt_template=prompt_template,
        description=data.get('description', ''),
    )
    return JsonResponse(_template_to_dict(t), status=201)


@admin_json_view
@require_http_methods(['GET', 'PUT', 'DELETE'])
def template_detail(request, pk):
    from django.shortcuts import get_object_or_404
    t = get_object_or_404(ContentTemplate, pk=pk)

    if request.method == 'GET':
        return JsonResponse(_template_to_dict(t))

    if request.method == 'PUT':
        data = request.json_data
        for field in ('name', 'platform', 'prompt_template', 'description'):
            if field in data:
                setattr(t, field, data[field])
        t.save()
        return JsonResponse(_template_to_dict(t))

    t.delete()
    return JsonResponse({}, status=204)


# -- History endpoint ----------------------------------------------------------

def _history_to_dict(h):
    return {
        'id': h.pk,
        'draft_id': h.draft_id,
        'prompt': h.prompt[:200],
        'model': h.model,
        'token_usage': h.token_usage,
        'created_at': h.created_at.isoformat() if h.created_at else None,
    }


@admin_json_view
@require_http_methods(['GET'])
def generation_history_list(request):
    draft_id = request.GET.get('draft_id')
    limit = int(request.GET.get('limit', 50))
    offset = int(request.GET.get('offset', 0))
    qs = GenerationHistory.objects.all()
    if draft_id:
        qs = qs.filter(draft_id=draft_id)
    total = qs.count()
    items = list(qs[offset:offset + limit])
    return JsonResponse({
        'history': [_history_to_dict(h) for h in items],
        'total': total,
    })


# -- Composer views ------------------------------------------------------------

def _get_draft_context(request, platform):
    draft_id = request.GET.get('draft_id')
    draft = None
    if draft_id:
        try:
            draft = ContentDraft.objects.get(pk=draft_id, platform=platform)
        except ContentDraft.DoesNotExist:
            pass
    return {
        'title': f'{platform.title()} Composer',
        'draft': draft,
        'draft_id': draft.pk if draft else None,
    }


@staff_member_required
def blog_composer(request):
    context = _get_draft_context(request, 'blog')
    return render(request, 'admin/assistant/blog_composer.html', context)


@staff_member_required
def linkedin_composer(request):
    context = _get_draft_context(request, 'linkedin')
    return render(request, 'admin/assistant/linkedin_composer.html', context)


@staff_member_required
def twitter_composer(request):
    context = _get_draft_context(request, 'twitter')
    return render(request, 'admin/assistant/twitter_composer.html', context)


# -- Dashboard -----------------------------------------------------------------

@staff_member_required
def assistant_dashboard(request):
    now = timezone.now()
    month_start = now.replace(day=1, hour=0, minute=0, second=0, microsecond=0)

    draft_counts = {
        'blog': ContentDraft.objects.filter(platform='blog').exclude(status='archived').count(),
        'linkedin': ContentDraft.objects.filter(platform='linkedin').exclude(status='archived').count(),
        'twitter': ContentDraft.objects.filter(platform='twitter').exclude(status='archived').count(),
    }

    monthly_history = GenerationHistory.objects.filter(created_at__gte=month_start)
    monthly_generations = monthly_history.count()

    monthly_input_tokens = 0
    monthly_output_tokens = 0
    for h in monthly_history.only('token_usage'):
        usage = h.token_usage or {}
        monthly_input_tokens += usage.get('input', 0)
        monthly_output_tokens += usage.get('output', 0)
    monthly_tokens = monthly_input_tokens + monthly_output_tokens

    input_cost = _get_config_float('assistant_input_token_cost', DEFAULT_INPUT_TOKEN_COST)
    output_cost = _get_config_float('assistant_output_token_cost', DEFAULT_OUTPUT_TOKEN_COST)
    estimated_cost = (monthly_input_tokens * input_cost) + (monthly_output_tokens * output_cost)

    monthly_budget = _get_config_float('assistant_monthly_token_budget', DEFAULT_MONTHLY_BUDGET)
    alert_threshold = _get_config_float('assistant_cost_alert_threshold', DEFAULT_ALERT_THRESHOLD)
    budget_usage_pct = (monthly_tokens / monthly_budget * 100) if monthly_budget > 0 else 0
    budget_warning = monthly_tokens >= (monthly_budget * alert_threshold)

    recent_drafts = ContentDraft.objects.order_by('-updated_at')[:10]

    context = {
        'title': 'Content Assistant',
        'draft_counts': draft_counts,
        'monthly_generations': monthly_generations,
        'monthly_tokens': monthly_tokens,
        'estimated_cost': round(estimated_cost, 4),
        'budget_usage_pct': round(budget_usage_pct, 1),
        'budget_warning': budget_warning,
        'recent_drafts': recent_drafts,
    }
    return render(request, 'admin/assistant/dashboard.html', context)
