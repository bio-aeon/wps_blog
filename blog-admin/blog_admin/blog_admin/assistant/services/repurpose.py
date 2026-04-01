from blog_admin.assistant.constants import VALID_PLATFORMS, PLATFORM_RULES
from blog_admin.assistant.services.draft import create_draft, draft_to_dict
from blog_admin.assistant.services.generation import (
    check_rate_limit, _log_generation, _get_system_prompt,
)
from blog_admin.assistant.services.llm_client import get_llm_client
from blog_admin.models import Post, Config


def _get_voice_preset(platform):
    config_name = f'assistant_voice_{platform}'
    defaults = {
        'blog': 'technical, detailed, code-heavy',
        'linkedin': 'professional, thought-leadership, concise',
        'twitter': 'conversational, punchy, engaging',
    }
    try:
        config = Config.objects.filter(name=config_name).first()
        if config and config.value:
            return config.value
    except Exception:
        pass
    return defaults.get(platform, '')


def repurpose_content(source_post_id, target_platform, language='en',
                      format='single', instructions=''):
    """Adapt a blog post for a different platform."""
    if target_platform not in VALID_PLATFORMS:
        raise ValueError(
            f"Invalid target_platform. Must be one of: "
            f"{', '.join(sorted(VALID_PLATFORMS))}"
        )

    check_rate_limit()

    try:
        post = Post.objects.get(pk=source_post_id)
    except Post.DoesNotExist:
        raise ValueError(f"Post with id {source_post_id} not found.")

    voice = _get_voice_preset(target_platform)
    rules = PLATFORM_RULES.get(target_platform, {})

    system_prompt = _get_system_prompt(target_platform)

    parts = [
        f"Adapt the following blog post for {target_platform}.",
        f"Voice/tone: {voice}.",
    ]

    if target_platform == 'linkedin':
        parts.append(
            f"Optimal length: ~{rules.get('optimal_chars', 1300)} characters. "
            f"Max: {rules.get('max_chars', 3000)} characters. "
            f"Include up to {rules.get('max_hashtags', 5)} hashtags."
        )
    elif target_platform == 'twitter':
        if format == 'thread':
            parts.append(
                f"Create a thread of tweets. "
                f"Each tweet max {rules.get('max_chars_per_tweet', 280)} chars. "
                f"Return a JSON object with a \"tweets\" array of strings."
            )
        else:
            parts.append(
                f"Create a single tweet, max {rules.get('max_chars_per_tweet', 280)} chars. "
                f"Return a JSON object with a \"tweet\" string."
            )

    if instructions:
        parts.append(f"Additional instructions: {instructions}")

    parts.append(
        f"\nSource blog post:\n"
        f"Title: {post.name}\n\n"
        f"{post.text[:3000]}"
    )

    if language != 'en':
        parts.append(f"Write the content in language: {language}.")

    user_prompt = '\n\n'.join(parts)
    client = get_llm_client(target_platform)

    is_twitter = (target_platform == 'twitter')
    if is_twitter:
        parsed, llm_response = client.generate_json(system_prompt, user_prompt)
        if format == 'thread':
            tweets = parsed.get('tweets', [])
            body = '\n\n'.join(
                f"[{i+1}/{len(tweets)}] {t}" for i, t in enumerate(tweets)
            )
            metadata = {'tweets': tweets, 'format': 'thread'}
        else:
            body = parsed.get('tweet', llm_response.content)
            metadata = {'format': 'single'}
    else:
        llm_response = client.generate(system_prompt, user_prompt)
        body = llm_response.content
        metadata = {}

    draft = create_draft(
        platform=target_platform,
        body=body,
        title=f"[Repurposed] {post.name}",
        language_code=language,
        status='draft',
        metadata=metadata,
        source_post_id=source_post_id,
    )

    _log_generation(draft, user_prompt, llm_response)

    result = draft_to_dict(draft)
    result['generation'] = {
        'model': llm_response.model,
        'tokens_used': {
            'input': llm_response.input_tokens,
            'output': llm_response.output_tokens,
        },
    }
    return result
