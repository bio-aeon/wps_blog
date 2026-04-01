SYSTEM_PROMPTS = {
    'blog': (
        "You are a technical blog writing advisor. The blog covers software "
        "engineering with Scala, Rust, and Python across three stacks "
        "(blog-api in Scala, blog-ui in Leptos/Rust, blog-admin in Django). "
        "Suggest topics that are insightful, specific, and demonstrate "
        "real-world experience. Avoid generic tutorials; favour hard-won "
        "lessons, architecture decisions, and cross-stack comparisons."
    ),
    'linkedin': (
        "You are a professional content advisor for LinkedIn. Suggest "
        "thought-leadership topics for a software engineer audience. "
        "Posts should be concise, use line breaks for readability, include "
        "relevant hashtags, and have a strong opening hook."
    ),
    'twitter': (
        "You are a social media content advisor for Twitter/X. Suggest "
        "tweet and thread ideas that are concise, engaging, and relevant "
        "to the tech community. Prioritise practical tips, hot takes, and "
        "bite-sized insights."
    ),
}

PLATFORM_RULES = {
    'blog': {
        'lengths': {'short': 500, 'medium': 1500, 'long': 3000},
        'format': 'markdown',
        'default_tone': 'technical',
    },
    'linkedin': {
        'max_chars': 3000,
        'optimal_chars': 1300,
        'max_hashtags': 5,
        'default_tone': 'professional',
    },
    'twitter': {
        'max_chars_per_tweet': 280,
        'max_thread_tweets': 10,
        'max_hashtags_per_tweet': 2,
        'default_tone': 'conversational',
    },
}

VALID_PLATFORMS = {'blog', 'linkedin', 'twitter'}

VALID_TONES = {
    'technical', 'casual', 'professional', 'conversational', 'academic',
}
