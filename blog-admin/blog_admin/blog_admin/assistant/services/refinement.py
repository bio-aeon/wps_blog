from blog_admin.assistant.constants import VALID_TONES
from blog_admin.assistant.services.draft import get_draft, draft_to_dict
from blog_admin.assistant.services.generation import (
    check_rate_limit, _log_generation, _get_system_prompt,
)
from blog_admin.assistant.services.llm_client import get_llm_client


def refine_content(draft_id, instructions):
    """Refine a draft's body using AI with specific instructions."""
    check_rate_limit()
    draft = get_draft(draft_id)

    system_prompt = (
        _get_system_prompt(draft.platform)
        + "\n\nYou are refining an existing draft. "
        "Return only the improved content, no commentary."
    )
    user_prompt = (
        f"Here is the current draft:\n\n{draft.body}\n\n"
        f"Instructions for improvement:\n{instructions}\n\n"
        f"Return the refined version of the full content."
    )

    client = get_llm_client(draft.platform)
    llm_response = client.generate(system_prompt, user_prompt)

    draft.body = llm_response.content
    draft.save()

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


def adjust_tone(draft_id, target_tone):
    """Rewrite a draft in a different tone."""
    if target_tone not in VALID_TONES:
        raise ValueError(
            f"Invalid tone '{target_tone}'. "
            f"Must be one of: {', '.join(sorted(VALID_TONES))}"
        )

    check_rate_limit()
    draft = get_draft(draft_id)

    system_prompt = (
        _get_system_prompt(draft.platform)
        + "\n\nYou are rewriting content in a different tone. "
        "Preserve the meaning and structure. "
        "Return only the rewritten content, no commentary."
    )
    user_prompt = (
        f"Rewrite the following content in a {target_tone} tone:\n\n"
        f"{draft.body}"
    )

    client = get_llm_client(draft.platform)
    llm_response = client.generate(system_prompt, user_prompt)

    draft.body = llm_response.content
    draft.save()

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


def seo_suggest(draft_id):
    """Generate SEO metadata suggestions for a blog draft."""
    draft = get_draft(draft_id)

    if draft.platform != 'blog':
        raise ValueError("SEO suggestions are only available for blog drafts.")

    check_rate_limit()

    system_prompt = (
        "You are an SEO specialist for a technical blog. "
        "Analyze the content and suggest optimized metadata."
    )
    user_prompt = (
        f"Analyze this blog post and suggest SEO metadata:\n\n"
        f"Title: {draft.title or '(untitled)'}\n\n"
        f"Content:\n{draft.body[:3000]}\n\n"
        f"Return a JSON object with:\n"
        f"- \"seo_title\": optimized title (max 60 chars)\n"
        f"- \"seo_description\": meta description (max 160 chars)\n"
        f"- \"seo_keywords\": array of 5-8 keyword strings\n"
        f"- \"suggestions\": array of 2-3 actionable SEO tips"
    )

    client = get_llm_client('blog')
    parsed, llm_response = client.generate_json(system_prompt, user_prompt)

    _log_generation(draft, user_prompt, llm_response)

    return {
        'draft_id': draft.pk,
        'seo_title': parsed.get('seo_title', ''),
        'seo_description': parsed.get('seo_description', ''),
        'seo_keywords': parsed.get('seo_keywords', []),
        'suggestions': parsed.get('suggestions', []),
        'generation': {
            'model': llm_response.model,
            'tokens_used': {
                'input': llm_response.input_tokens,
                'output': llm_response.output_tokens,
            },
        },
    }
