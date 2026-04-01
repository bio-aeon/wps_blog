"""
Platform-specific content validation.

Returns lists of warnings (strings). An empty list means content is valid.
"""

from blog_admin.assistant.constants import PLATFORM_RULES


def validate_blog_output(content, target_length='medium'):
    """Validate blog post content against platform rules."""
    rules = PLATFORM_RULES['blog']
    target_words = rules['lengths'].get(target_length, 1500)
    word_count = len(content.split())
    warnings = []

    if word_count < target_words * 0.5:
        warnings.append(
            f"Content is {word_count} words, target was ~{target_words} "
            f"(under 50% of target)."
        )
    elif word_count > target_words * 1.5:
        warnings.append(
            f"Content is {word_count} words, target was ~{target_words} "
            f"(over 150% of target)."
        )

    if not content.lstrip().startswith('#'):
        warnings.append("Content does not start with a Markdown heading.")

    return warnings


def validate_linkedin_output(content):
    """Validate LinkedIn post content against platform rules."""
    rules = PLATFORM_RULES['linkedin']
    max_chars = rules['max_chars']
    warnings = []

    if len(content) > max_chars:
        warnings.append(
            f"Content is {len(content)} characters, exceeds LinkedIn "
            f"max of {max_chars}."
        )

    if '#' not in content:
        warnings.append("No hashtags found in content.")

    return warnings


def validate_twitter_output(tweets):
    """Validate tweet list against platform rules."""
    rules = PLATFORM_RULES['twitter']
    max_chars = rules['max_chars_per_tweet']
    warnings = []

    for i, tweet in enumerate(tweets):
        if not tweet.strip():
            warnings.append(f"Tweet {i + 1} is empty.")
        elif len(tweet) > max_chars:
            warnings.append(
                f"Tweet {i + 1} is {len(tweet)} characters, exceeds "
                f"max of {max_chars}."
            )

    return warnings


def split_into_tweets(long_text, max_chars=280):
    """Split text into tweets at sentence boundaries."""
    import re
    sentences = re.split(r'(?<=[.!?])\s+', long_text.strip())
    tweets = []
    current = ''

    for sentence in sentences:
        if not sentence:
            continue
        candidate = (current + ' ' + sentence).strip() if current else sentence
        if len(candidate) <= max_chars:
            current = candidate
        else:
            if current:
                tweets.append(current)
            if len(sentence) <= max_chars:
                current = sentence
            else:
                # Sentence itself exceeds limit — force-split on word boundary
                words = sentence.split()
                current = ''
                for word in words:
                    candidate = (current + ' ' + word).strip() if current else word
                    if len(candidate) <= max_chars:
                        current = candidate
                    else:
                        if current:
                            tweets.append(current)
                        current = word

    if current:
        tweets.append(current)

    return tweets
