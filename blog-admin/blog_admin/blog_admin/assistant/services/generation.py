from datetime import timedelta

from django.utils import timezone

from blog_admin.assistant.constants import SYSTEM_PROMPTS, PLATFORM_RULES
from blog_admin.assistant.services.llm_client import get_llm_client
from blog_admin.assistant.services.draft import create_draft, draft_to_dict
from blog_admin.models import Config, GenerationHistory, Post


class RateLimitExceeded(Exception):
    pass


DEFAULT_RATE_LIMIT_PER_HOUR = 50
DEFAULT_RATE_WINDOW_SECONDS = 3600


def _get_rate_limit():
    try:
        config = Config.objects.filter(
            name='assistant_rate_limit_per_hour'
        ).first()
        if config and config.value:
            return int(config.value)
    except Exception:
        pass
    return DEFAULT_RATE_LIMIT_PER_HOUR


def _get_rate_window():
    try:
        config = Config.objects.filter(
            name='assistant_rate_limit_window_seconds'
        ).first()
        if config and config.value:
            return int(config.value)
    except Exception:
        pass
    return DEFAULT_RATE_WINDOW_SECONDS


def check_rate_limit():
    """Raise RateLimitExceeded if generation count exceeds threshold."""
    limit = _get_rate_limit()
    window = _get_rate_window()
    since = timezone.now() - timedelta(seconds=window)
    count = GenerationHistory.objects.filter(created_at__gte=since).count()
    if count >= limit:
        raise RateLimitExceeded(
            f"Rate limit exceeded: {count}/{limit} generations in the "
            f"last {window} seconds."
        )


def _log_generation(draft, prompt, llm_response):
    return GenerationHistory.objects.create(
        draft=draft,
        prompt=prompt,
        model=llm_response.model,
        response=llm_response.content,
        token_usage={
            'input': llm_response.input_tokens,
            'output': llm_response.output_tokens,
        },
    )


def _get_system_prompt(platform):
    config_name = f'assistant_system_prompt_{platform}'
    try:
        config = Config.objects.filter(name=config_name).first()
        if config and config.value:
            return config.value
    except Exception:
        pass
    return SYSTEM_PROMPTS.get(platform, '')


def _build_blog_prompt(params):
    rules = PLATFORM_RULES['blog']
    target_length = params.get('target_length', 'medium')
    word_count = rules['lengths'].get(target_length, 1500)
    tone = params.get('tone', rules['default_tone'])

    parts = [f"Write a blog post about: {params['topic']}"]
    parts.append(f"Target length: ~{word_count} words.")
    parts.append(f"Tone: {tone}.")
    parts.append("Format: Markdown with headings, paragraphs, and code blocks.")

    outline = params.get('outline')
    if outline:
        parts.append("Follow this outline:\n" + '\n'.join(
            f"- {section}" for section in outline
        ))

    if params.get('include_code_examples'):
        parts.append(
            "Include relevant code examples with language-tagged fenced blocks."
        )

    language = params.get('language', 'en')
    if language != 'en':
        parts.append(f"Write the content in language: {language}.")

    return '\n\n'.join(parts)


def _build_linkedin_prompt(params):
    rules = PLATFORM_RULES['linkedin']
    tone = params.get('tone', rules['default_tone'])

    parts = [f"Write a LinkedIn post about: {params['topic']}"]
    parts.append(f"Tone: {tone}.")
    parts.append(
        f"Optimal length: ~{rules['optimal_chars']} characters "
        f"(max {rules['max_chars']})."
    )
    parts.append("Use line breaks for readability.")
    parts.append("Start with a strong hook line.")

    if params.get('include_hashtags', True):
        parts.append(
            f"Include {rules['max_hashtags']} relevant hashtags at the end."
        )

    source_post_id = params.get('source_post_id')
    if source_post_id:
        try:
            post = Post.objects.get(pk=source_post_id)
            parts.append(
                f"Base this on the following blog post:\n"
                f"Title: {post.name}\n"
                f"Content: {post.text[:2000]}"
            )
        except Post.DoesNotExist:
            pass

    language = params.get('language', 'en')
    if language != 'en':
        parts.append(f"Write the content in language: {language}.")

    return '\n\n'.join(parts)


def _build_twitter_prompt(params):
    rules = PLATFORM_RULES['twitter']
    tweet_format = params.get('format', 'single')

    parts = [f"Write a {'tweet thread' if tweet_format == 'thread' else 'tweet'} "
             f"about: {params['topic']}"]

    if tweet_format == 'thread':
        max_tweets = params.get('max_tweets', rules['max_thread_tweets'])
        parts.append(
            f"Create a thread of up to {max_tweets} tweets.\n"
            f"Each tweet MUST be {rules['max_chars_per_tweet']} characters or fewer.\n"
            "Each tweet should stand alone but flow as a narrative.\n"
            "Return a JSON object with a \"tweets\" array of strings."
        )
    else:
        parts.append(
            f"The tweet MUST be {rules['max_chars_per_tweet']} characters or fewer.\n"
            "Return a JSON object with a \"tweet\" string."
        )

    if params.get('include_hashtags', True):
        parts.append(
            f"Include up to {rules['max_hashtags_per_tweet']} hashtags per tweet."
        )

    language = params.get('language', 'en')
    if language != 'en':
        parts.append(f"Write the content in language: {language}.")

    return '\n\n'.join(parts)


PROMPT_BUILDERS = {
    'blog': _build_blog_prompt,
    'linkedin': _build_linkedin_prompt,
    'twitter': _build_twitter_prompt,
}


def generate_content(platform, params):
    """
    Generate content for the given platform.

    Creates a ContentDraft, calls the LLM, logs to GenerationHistory,
    and returns the draft data with generation metadata.
    """
    check_rate_limit()

    if 'topic' not in params:
        raise ValueError("'topic' is required.")

    system_prompt = _get_system_prompt(platform)
    build_prompt = PROMPT_BUILDERS[platform]
    user_prompt = build_prompt(params)

    client = get_llm_client(platform)

    tweet_format = params.get('format', 'single')
    is_twitter_structured = (platform == 'twitter')

    if is_twitter_structured:
        parsed, llm_response = client.generate_json(system_prompt, user_prompt)
        if tweet_format == 'thread':
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

    if platform == 'linkedin' and params.get('include_hashtags', True):
        lines = body.strip().split('\n')
        for line in reversed(lines):
            if '#' in line:
                hashtags = [w for w in line.split() if w.startswith('#')]
                if hashtags:
                    metadata['hashtags'] = hashtags
                break

    title = params.get('topic')
    language = params.get('language', 'en')

    draft = create_draft(
        platform=platform,
        body=body,
        title=title,
        language_code=language,
        status='draft',
        metadata=metadata,
        source_post_id=params.get('source_post_id'),
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
