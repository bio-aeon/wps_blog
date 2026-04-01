import json

from blog_admin.assistant.constants import SYSTEM_PROMPTS, PLATFORM_RULES
from blog_admin.assistant.services.llm_client import get_llm_client
from blog_admin.models import Post, ContentDraft, Config


def _get_system_prompt(platform):
    """Return system prompt from configs (if overridden) or constants."""
    config_name = f'assistant_system_prompt_{platform}'
    try:
        config = Config.objects.filter(name=config_name).first()
        if config and config.value:
            return config.value
    except Exception:
        pass
    return SYSTEM_PROMPTS.get(platform, '')


def _build_context(platform, language, focus_areas, avoid_topics):
    """Build the user prompt with context from existing content."""
    existing_titles = list(
        Post.objects.filter(is_hidden=False)
        .values_list('name', flat=True)[:50]
    )
    draft_titles = list(
        ContentDraft.objects.filter(platform=platform)
        .exclude(status='archived')
        .exclude(title__isnull=True)
        .values_list('title', flat=True)[:30]
    )

    parts = []
    if existing_titles:
        parts.append(
            "Existing published posts (avoid repeating these topics):\n"
            + '\n'.join(f"- {t}" for t in existing_titles)
        )
    if draft_titles:
        parts.append(
            "Current drafts in progress:\n"
            + '\n'.join(f"- {t}" for t in draft_titles)
        )
    if focus_areas:
        parts.append("Focus areas: " + ', '.join(focus_areas))
    if avoid_topics:
        parts.append("Topics to avoid: " + ', '.join(avoid_topics))

    rules = PLATFORM_RULES.get(platform, {})
    if platform == 'blog':
        parts.append(
            f"Length options: {', '.join(rules.get('lengths', {}).keys())}. "
            f"Format: {rules.get('format', 'markdown')}."
        )
    elif platform == 'linkedin':
        parts.append(
            f"Optimal post length: ~{rules.get('optimal_chars', 1300)} characters. "
            f"Max hashtags: {rules.get('max_hashtags', 5)}."
        )
    elif platform == 'twitter':
        parts.append(
            f"Max {rules.get('max_chars_per_tweet', 280)} characters per tweet. "
            f"Thread limit: {rules.get('max_thread_tweets', 10)} tweets."
        )

    parts.append(f"Language: {language}")
    return '\n\n'.join(parts)


def suggest_topics(platform, count=5, language='en',
                   focus_areas=None, avoid_topics=None):
    """
    Ask the LLM to suggest topics for the given platform.

    Returns a dict with a 'suggestions' key containing a list of dicts,
    each with: topic, brief, platform, tags, target_length.
    """
    system_prompt = _get_system_prompt(platform)
    context = _build_context(
        platform, language,
        focus_areas=focus_areas or [],
        avoid_topics=avoid_topics or [],
    )

    user_prompt = (
        f"Suggest exactly {count} content ideas for {platform}.\n\n"
        f"{context}\n\n"
        f"Return a JSON object with a single key \"suggestions\" containing "
        f"an array of objects. Each object must have: "
        f"\"topic\" (string), \"brief\" (1-2 sentence description), "
        f"\"platform\" (\"{platform}\"), \"tags\" (array of strings), "
        f"\"target_length\" (\"short\", \"medium\", or \"long\")."
    )

    client = get_llm_client(platform)
    parsed, response = client.generate_json(system_prompt, user_prompt)

    return {
        'suggestions': parsed.get('suggestions', []),
        'generation': {
            'model': response.model,
            'tokens_used': {
                'input': response.input_tokens,
                'output': response.output_tokens,
            },
        },
    }
