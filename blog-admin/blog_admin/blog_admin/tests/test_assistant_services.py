import json
from unittest.mock import patch, MagicMock

from django.test import TestCase
from model_bakery import baker

from blog_admin.assistant.services.llm_client import (
    LLMResponse, ClaudeClient, get_llm_client, _get_model_for_platform,
)
from blog_admin.assistant.services.suggestion import (
    suggest_topics, _build_context, _get_system_prompt,
)
from blog_admin.assistant.services.generation import (
    generate_content, check_rate_limit, RateLimitExceeded,
)
from blog_admin.assistant.services.refinement import (
    refine_content, adjust_tone, seo_suggest,
)
from blog_admin.assistant.services.validation import (
    validate_blog_output, validate_linkedin_output,
    validate_twitter_output, split_into_tweets,
)
from blog_admin.assistant.services.repurpose import (
    repurpose_content as repurpose_svc,
)
from blog_admin.models import (
    Config, Language, Post, User, ContentDraft, GenerationHistory,
)


def _mock_anthropic_response(text='Generated content',
                             input_tokens=100, output_tokens=200):
    """Build a mock Anthropic Messages API response."""
    mock_response = MagicMock()
    mock_response.content = [MagicMock(text=text)]
    mock_response.usage.input_tokens = input_tokens
    mock_response.usage.output_tokens = output_tokens
    return mock_response


class LLMResponseTest(TestCase):

    def test_exposes_content_model_and_token_counts(self):
        r = LLMResponse(
            content='hi', model='claude-sonnet-4-6',
            input_tokens=10, output_tokens=20,
        )
        self.assertEqual(r.content, 'hi')
        self.assertEqual(r.model, 'claude-sonnet-4-6')
        self.assertEqual(r.input_tokens, 10)
        self.assertEqual(r.output_tokens, 20)


class ClaudeClientTest(TestCase):

    @patch('blog_admin.assistant.services.llm_client.anthropic.Anthropic')
    def test_returns_response_with_content_and_tokens(self, MockAnthropic):
        MockAnthropic.return_value.messages.create.return_value = (
            _mock_anthropic_response('Hello world', 50, 75)
        )

        client = ClaudeClient(api_key='sk-test', model='claude-sonnet-4-6')
        result = client.generate('system', 'user prompt')

        self.assertIsInstance(result, LLMResponse)
        self.assertEqual(result.content, 'Hello world')
        self.assertEqual(result.model, 'claude-sonnet-4-6')
        self.assertEqual(result.input_tokens, 50)
        self.assertEqual(result.output_tokens, 75)

        MockAnthropic.return_value.messages.create.assert_called_once()
        call_kwargs = MockAnthropic.return_value.messages.create.call_args[1]
        self.assertEqual(call_kwargs['system'], 'system')
        self.assertEqual(call_kwargs['messages'], [
            {'role': 'user', 'content': 'user prompt'},
        ])

    @patch('blog_admin.assistant.services.llm_client.anthropic.Anthropic')
    def test_parses_valid_json_response(self, MockAnthropic):
        json_str = json.dumps({'suggestions': ['topic1', 'topic2']})
        MockAnthropic.return_value.messages.create.return_value = (
            _mock_anthropic_response(json_str)
        )

        client = ClaudeClient(api_key='sk-test')
        parsed, response = client.generate_json('system', 'give me topics')

        self.assertEqual(parsed, {'suggestions': ['topic1', 'topic2']})
        self.assertIsInstance(response, LLMResponse)

    @patch('blog_admin.assistant.services.llm_client.anthropic.Anthropic')
    def test_strips_markdown_fences_from_json(self, MockAnthropic):
        json_str = '```json\n{"key": "value"}\n```'
        MockAnthropic.return_value.messages.create.return_value = (
            _mock_anthropic_response(json_str)
        )

        client = ClaudeClient(api_key='sk-test')
        parsed, _ = client.generate_json('system', 'prompt')

        self.assertEqual(parsed, {'key': 'value'})

    @patch('blog_admin.assistant.services.llm_client.anthropic.Anthropic')
    def test_rejects_non_json_response(self, MockAnthropic):
        MockAnthropic.return_value.messages.create.return_value = (
            _mock_anthropic_response('not valid json at all')
        )

        client = ClaudeClient(api_key='sk-test')
        with self.assertRaises(ValueError) as ctx:
            client.generate_json('system', 'prompt')
        self.assertIn('invalid JSON', str(ctx.exception))


class GetModelForPlatformTest(TestCase):

    def test_returns_default_sonnet(self):
        self.assertEqual(
            _get_model_for_platform('blog'), 'claude-sonnet-4-6'
        )

    def test_reads_override_from_config(self):
        Config.objects.create(
            name='assistant_model_blog',
            value='claude-opus-4-6',
            comment='test',
        )
        self.assertEqual(_get_model_for_platform('blog'), 'claude-opus-4-6')


class GetLLMClientTest(TestCase):

    @patch('blog_admin.assistant.services.llm_client.get_llm_api_key',
           return_value='sk-test')
    def test_returns_claude_client(self, _mock_key):
        client = get_llm_client('blog')
        self.assertIsInstance(client, ClaudeClient)
        self.assertEqual(client.model, 'claude-sonnet-4-6')


# -- Suggestion service tests --------------------------------------------------

MOCK_SUGGESTIONS_JSON = json.dumps({
    'suggestions': [
        {
            'topic': 'Rust ownership explained',
            'brief': 'A deep dive into ownership.',
            'platform': 'blog',
            'tags': ['Rust'],
            'target_length': 'medium',
        },
        {
            'topic': 'Scala implicits vs Rust traits',
            'brief': 'Cross-language comparison.',
            'platform': 'blog',
            'tags': ['Scala', 'Rust'],
            'target_length': 'long',
        },
    ],
})


class SuggestionServiceTest(TestCase):

    def setUp(self):
        self.lang = baker.make(Language, code='en', name='English')

    @patch('blog_admin.assistant.services.suggestion.get_llm_client')
    def test_returns_topics_with_metadata(self, mock_get_client):
        mock_client = MagicMock()
        mock_client.generate_json.return_value = (
            json.loads(MOCK_SUGGESTIONS_JSON),
            LLMResponse('...', 'claude-sonnet-4-6', 100, 200),
        )
        mock_get_client.return_value = mock_client

        result = suggest_topics('blog', count=2, language='en')

        self.assertEqual(len(result['suggestions']), 2)
        self.assertEqual(result['suggestions'][0]['topic'], 'Rust ownership explained')
        self.assertEqual(result['generation']['model'], 'claude-sonnet-4-6')

    @patch('blog_admin.assistant.services.suggestion.get_llm_client')
    def test_dispatches_to_requested_platform(self, mock_get_client):
        mock_client = MagicMock()
        mock_client.generate_json.return_value = (
            {'suggestions': []},
            LLMResponse('', 'model', 0, 0),
        )
        mock_get_client.return_value = mock_client

        suggest_topics('linkedin', count=3)
        mock_get_client.assert_called_once_with('linkedin')

    @patch('blog_admin.assistant.services.suggestion.get_llm_client')
    def test_includes_focus_areas_in_prompt(self, mock_get_client):
        mock_client = MagicMock()
        mock_client.generate_json.return_value = (
            {'suggestions': []},
            LLMResponse('', 'model', 0, 0),
        )
        mock_get_client.return_value = mock_client

        suggest_topics('blog', focus_areas=['Rust', 'WASM'])

        call_args = mock_client.generate_json.call_args[0]
        user_prompt = call_args[1]
        self.assertIn('Rust', user_prompt)
        self.assertIn('WASM', user_prompt)

    def test_build_context_includes_existing_posts(self):
        user = User.objects.create_superuser('admin', 'a@b.com', 'pass')
        Post.objects.create(
            name='Existing Post', short_text='s', text='t',
            author=user, meta_title='', meta_keywords='',
            meta_description='', is_hidden=False,
        )
        context = _build_context('blog', 'en', [], [])
        self.assertIn('Existing Post', context)

    def test_build_context_includes_draft_titles(self):
        ContentDraft.objects.create(
            platform='blog', title='WIP Draft', body='x',
            language=self.lang,
        )
        context = _build_context('blog', 'en', [], [])
        self.assertIn('WIP Draft', context)

    def test_build_context_includes_avoid_topics(self):
        context = _build_context('blog', 'en', [], ['Old Topic'])
        self.assertIn('Old Topic', context)


# -- Generation service tests --------------------------------------------------

class GenerationServiceTest(TestCase):

    def setUp(self):
        self.lang = baker.make(Language, code='en', name='English')

    @patch('blog_admin.assistant.services.generation.get_llm_client')
    def test_creates_blog_draft_with_metadata(self, mock_get_client):
        mock_client = MagicMock()
        mock_client.generate.return_value = LLMResponse(
            '# My Post\n\nContent here.', 'claude-sonnet-4-6', 100, 300,
        )
        mock_get_client.return_value = mock_client

        result = generate_content('blog', {
            'topic': 'Rust ownership',
            'language': 'en',
        })

        self.assertEqual(result['platform'], 'blog')
        self.assertEqual(result['title'], 'Rust ownership')
        self.assertEqual(result['status'], 'draft')
        self.assertIn('generation', result)
        self.assertEqual(result['generation']['model'], 'claude-sonnet-4-6')
        self.assertEqual(ContentDraft.objects.count(), 1)

    @patch('blog_admin.assistant.services.generation.get_llm_client')
    def test_includes_outline_sections_in_prompt(self, mock_get_client):
        mock_client = MagicMock()
        mock_client.generate.return_value = LLMResponse(
            'content', 'model', 50, 100,
        )
        mock_get_client.return_value = mock_client

        generate_content('blog', {
            'topic': 'Testing',
            'outline': ['Intro', 'Unit tests', 'Conclusion'],
            'language': 'en',
        })

        call_args = mock_client.generate.call_args[0]
        user_prompt = call_args[1]
        self.assertIn('Intro', user_prompt)
        self.assertIn('Unit tests', user_prompt)

    @patch('blog_admin.assistant.services.generation.get_llm_client')
    def test_logs_generation_history_with_tokens(self, mock_get_client):
        mock_client = MagicMock()
        mock_client.generate.return_value = LLMResponse(
            'content', 'claude-sonnet-4-6', 50, 100,
        )
        mock_get_client.return_value = mock_client

        generate_content('blog', {'topic': 'Test', 'language': 'en'})

        self.assertEqual(GenerationHistory.objects.count(), 1)
        history = GenerationHistory.objects.first()
        self.assertEqual(history.model, 'claude-sonnet-4-6')
        self.assertEqual(history.token_usage, {'input': 50, 'output': 100})

    @patch('blog_admin.assistant.services.generation.get_llm_client')
    def test_creates_linkedin_draft_with_hashtags(self, mock_get_client):
        mock_client = MagicMock()
        mock_client.generate.return_value = LLMResponse(
            'Post content\n\n#Scala #FP', 'model', 40, 80,
        )
        mock_get_client.return_value = mock_client

        result = generate_content('linkedin', {
            'topic': 'Scala tips',
            'language': 'en',
        })

        self.assertEqual(result['platform'], 'linkedin')
        self.assertIn('hashtags', result['metadata'])

    @patch('blog_admin.assistant.services.generation.get_llm_client')
    def test_creates_single_tweet_draft(self, mock_get_client):
        mock_client = MagicMock()
        mock_client.generate_json.return_value = (
            {'tweet': 'Quick tip: use Rust for safety! #Rust'},
            LLMResponse('...', 'model', 30, 50),
        )
        mock_get_client.return_value = mock_client

        result = generate_content('twitter', {
            'topic': 'Rust tip',
            'format': 'single',
            'language': 'en',
        })

        self.assertEqual(result['platform'], 'twitter')
        self.assertEqual(result['metadata']['format'], 'single')

    @patch('blog_admin.assistant.services.generation.get_llm_client')
    def test_creates_thread_draft_with_tweet_array(self, mock_get_client):
        tweets = ['Tweet 1 #Rust', 'Tweet 2 about safety', 'Tweet 3 conclusion']
        mock_client = MagicMock()
        mock_client.generate_json.return_value = (
            {'tweets': tweets},
            LLMResponse('...', 'model', 30, 80),
        )
        mock_get_client.return_value = mock_client

        result = generate_content('twitter', {
            'topic': 'Rust thread',
            'format': 'thread',
            'language': 'en',
        })

        self.assertEqual(result['metadata']['format'], 'thread')
        self.assertEqual(result['metadata']['tweets'], tweets)

    def test_rejects_missing_topic(self):
        with self.assertRaises(ValueError):
            generate_content('blog', {'language': 'en'})

    @patch('blog_admin.assistant.services.generation.get_llm_client')
    def test_saves_draft_in_requested_language(self, mock_get_client):
        baker.make(Language, code='ru', name='Russian')
        mock_client = MagicMock()
        mock_client.generate.return_value = LLMResponse(
            'Контент', 'model', 40, 80,
        )
        mock_get_client.return_value = mock_client

        result = generate_content('blog', {
            'topic': 'Тема', 'language': 'ru',
        })
        self.assertEqual(result['language_code'], 'ru')


class RateLimitTest(TestCase):

    def setUp(self):
        self.lang = baker.make(Language, code='en', name='English')

    def test_rate_limit_allows_when_under(self):
        check_rate_limit()

    def test_rate_limit_blocks_when_exceeded(self):
        Config.objects.create(
            name='assistant_rate_limit_per_hour',
            value='2',
            comment='test',
        )
        draft = ContentDraft.objects.create(
            platform='blog', body='x', language=self.lang,
        )
        GenerationHistory.objects.create(
            draft=draft, prompt='p', model='m', response='r',
        )
        GenerationHistory.objects.create(
            draft=draft, prompt='p', model='m', response='r',
        )
        with self.assertRaises(RateLimitExceeded):
            check_rate_limit()


# -- Refinement service tests --------------------------------------------------

class RefineContentTest(TestCase):

    def setUp(self):
        self.lang = baker.make(Language, code='en', name='English')
        self.draft = ContentDraft.objects.create(
            platform='blog', title='Draft', body='Original body',
            language=self.lang,
        )

    @patch('blog_admin.assistant.services.refinement.get_llm_client')
    def test_updates_draft_body(self, mock_get_client):
        mock_client = MagicMock()
        mock_client.generate.return_value = LLMResponse(
            'Improved body', 'model', 80, 120,
        )
        mock_get_client.return_value = mock_client

        result = refine_content(self.draft.pk, 'Make it clearer')

        self.assertEqual(result['body'], 'Improved body')
        self.draft.refresh_from_db()
        self.assertEqual(self.draft.body, 'Improved body')

    @patch('blog_admin.assistant.services.refinement.get_llm_client')
    def test_logs_generation_history(self, mock_get_client):
        mock_client = MagicMock()
        mock_client.generate.return_value = LLMResponse(
            'Better', 'model', 50, 70,
        )
        mock_get_client.return_value = mock_client

        refine_content(self.draft.pk, 'Improve')

        self.assertEqual(GenerationHistory.objects.count(), 1)
        history = GenerationHistory.objects.first()
        self.assertEqual(history.draft_id, self.draft.pk)


class AdjustToneTest(TestCase):

    def setUp(self):
        self.lang = baker.make(Language, code='en', name='English')
        self.draft = ContentDraft.objects.create(
            platform='blog', title='Draft', body='Technical body',
            language=self.lang,
        )

    @patch('blog_admin.assistant.services.refinement.get_llm_client')
    def test_rewrites_body_in_target_tone(self, mock_get_client):
        mock_client = MagicMock()
        mock_client.generate.return_value = LLMResponse(
            'Casual body', 'model', 60, 90,
        )
        mock_get_client.return_value = mock_client

        result = adjust_tone(self.draft.pk, 'casual')

        self.assertEqual(result['body'], 'Casual body')

    def test_rejects_unsupported_tone(self):
        with self.assertRaises(ValueError) as ctx:
            adjust_tone(self.draft.pk, 'angry')
        self.assertIn('Invalid tone', str(ctx.exception))


class SEOSuggestTest(TestCase):

    def setUp(self):
        self.lang = baker.make(Language, code='en', name='English')

    @patch('blog_admin.assistant.services.refinement.get_llm_client')
    def test_returns_title_description_and_keywords(self, mock_get_client):
        draft = ContentDraft.objects.create(
            platform='blog', title='Rust Tips', body='Content about Rust',
            language=self.lang,
        )
        mock_client = MagicMock()
        mock_client.generate_json.return_value = (
            {
                'seo_title': 'Top Rust Tips',
                'seo_description': 'Learn Rust best practices.',
                'seo_keywords': ['Rust', 'programming'],
                'suggestions': ['Add more keywords'],
            },
            LLMResponse('...', 'model', 70, 100),
        )
        mock_get_client.return_value = mock_client

        result = seo_suggest(draft.pk)

        self.assertEqual(result['seo_title'], 'Top Rust Tips')
        self.assertIsInstance(result['seo_keywords'], list)
        self.assertIn('generation', result)

    def test_rejects_non_blog_drafts(self):
        draft = ContentDraft.objects.create(
            platform='linkedin', body='x', language=self.lang,
        )
        with self.assertRaises(ValueError) as ctx:
            seo_suggest(draft.pk)
        self.assertIn('blog', str(ctx.exception))


# -- Repurpose service tests ---------------------------------------------------

class RepurposeContentTest(TestCase):

    def setUp(self):
        self.lang = baker.make(Language, code='en', name='English')
        self.user = User.objects.create_superuser('admin', 'a@b.com', 'pass')
        self.post = Post.objects.create(
            name='Original Post', short_text='Short', text='Full blog content',
            author=self.user, meta_title='t', meta_keywords='k',
            meta_description='d', is_hidden=False,
        )

    @patch('blog_admin.assistant.services.repurpose.get_llm_client')
    def test_creates_linkedin_draft_from_blog_post(self, mock_get_client):
        mock_client = MagicMock()
        mock_client.generate.return_value = LLMResponse(
            'LinkedIn version of the post', 'model', 80, 150,
        )
        mock_get_client.return_value = mock_client

        result = repurpose_svc(
            source_post_id=self.post.pk,
            target_platform='linkedin',
        )

        self.assertEqual(result['platform'], 'linkedin')
        self.assertEqual(result['source_post_id'], self.post.pk)
        self.assertIn('Repurposed', result['title'])
        self.assertEqual(ContentDraft.objects.count(), 1)
        self.assertEqual(GenerationHistory.objects.count(), 1)

    @patch('blog_admin.assistant.services.repurpose.get_llm_client')
    def test_creates_twitter_thread_from_blog_post(self, mock_get_client):
        tweets = ['Thread tweet 1', 'Thread tweet 2']
        mock_client = MagicMock()
        mock_client.generate_json.return_value = (
            {'tweets': tweets},
            LLMResponse('...', 'model', 60, 100),
        )
        mock_get_client.return_value = mock_client

        result = repurpose_svc(
            source_post_id=self.post.pk,
            target_platform='twitter',
            format='thread',
        )

        self.assertEqual(result['platform'], 'twitter')
        self.assertEqual(result['metadata']['format'], 'thread')

    def test_rejects_nonexistent_source_post(self):
        with self.assertRaises(ValueError):
            repurpose_svc(source_post_id=99999, target_platform='linkedin')

    def test_rejects_invalid_target_platform(self):
        with self.assertRaises(ValueError):
            repurpose_svc(
                source_post_id=self.post.pk,
                target_platform='facebook',
            )

    @patch('blog_admin.assistant.services.repurpose.get_llm_client')
    def test_includes_voice_preset_in_prompt(self, mock_get_client):
        mock_client = MagicMock()
        mock_client.generate.return_value = LLMResponse(
            'Adapted content', 'model', 80, 150,
        )
        mock_get_client.return_value = mock_client

        repurpose_svc(
            source_post_id=self.post.pk,
            target_platform='linkedin',
        )

        call_args = mock_client.generate.call_args[0]
        user_prompt = call_args[1]
        self.assertIn('professional', user_prompt.lower())


# -- Validation function tests -------------------------------------------------

class ValidateBlogOutputTest(TestCase):

    def test_warns_when_under_50_pct_of_short_target(self):
        content = '# Title\n\n' + 'word ' * 100  # ~100 words
        warnings = validate_blog_output(content, 'short')  # target ~500
        self.assertTrue(any('under 50%' in w for w in warnings))

    def test_warns_when_under_50_pct_of_long_target(self):
        content = '# Title\n\n' + 'word ' * 500  # ~500 words
        warnings = validate_blog_output(content, 'long')  # target ~3000
        self.assertTrue(any('under 50%' in w for w in warnings))

    def test_no_length_warning_when_on_target(self):
        content = '# Title\n\n' + 'word ' * 1400  # ~1400 words, target 1500
        warnings = validate_blog_output(content, 'medium')
        length_warnings = [w for w in warnings if 'words' in w]
        self.assertEqual(length_warnings, [])

    def test_warns_when_missing_markdown_heading(self):
        content = 'No heading here, just text.'
        warnings = validate_blog_output(content, 'medium')
        self.assertTrue(any('Markdown heading' in w for w in warnings))

    def test_no_heading_warning_with_valid_markdown(self):
        content = '# Title\n\n' + 'word ' * 1400
        warnings = validate_blog_output(content, 'medium')
        heading_warnings = [w for w in warnings if 'heading' in w.lower()]
        self.assertEqual(heading_warnings, [])


class ValidateLinkedinOutputTest(TestCase):

    def test_warns_when_exceeding_char_limit(self):
        content = 'x' * 3500
        warnings = validate_linkedin_output(content)
        self.assertTrue(any('exceeds' in w for w in warnings))

    def test_warns_when_no_hashtags_found(self):
        content = 'Great post without any hashtags.'
        warnings = validate_linkedin_output(content)
        self.assertTrue(any('hashtag' in w.lower() for w in warnings))

    def test_passes_valid_content(self):
        content = 'Great content about Scala. #Scala #FP'
        warnings = validate_linkedin_output(content)
        self.assertEqual(warnings, [])


class ValidateTwitterOutputTest(TestCase):

    def test_warns_when_tweet_exceeds_limit(self):
        tweets = ['x' * 300]
        warnings = validate_twitter_output(tweets)
        self.assertTrue(any('exceeds' in w for w in warnings))

    def test_warns_on_empty_tweet(self):
        tweets = ['Good tweet', '   ', 'Another']
        warnings = validate_twitter_output(tweets)
        self.assertTrue(any('empty' in w.lower() for w in warnings))

    def test_passes_valid_tweets(self):
        tweets = ['Short tweet #Rust', 'Another valid tweet']
        warnings = validate_twitter_output(tweets)
        self.assertEqual(warnings, [])


class SplitIntoTweetsTest(TestCase):

    def test_split_at_sentence_boundaries(self):
        text = 'First sentence. Second sentence. Third sentence.'
        tweets = split_into_tweets(text, max_chars=40)
        self.assertTrue(len(tweets) >= 2)
        for t in tweets:
            self.assertLessEqual(len(t), 40)

    def test_single_short_text_stays_together(self):
        text = 'Just a short tweet.'
        tweets = split_into_tweets(text, max_chars=280)
        self.assertEqual(len(tweets), 1)
        self.assertEqual(tweets[0], text)

    def test_respects_char_limit(self):
        text = 'A' * 100 + '. ' + 'B' * 100 + '. ' + 'C' * 100 + '.'
        tweets = split_into_tweets(text, max_chars=120)
        for t in tweets:
            self.assertLessEqual(len(t), 120)


# -- System prompt management tests (Step 14) ---------------------------------

class SystemPromptManagementTest(TestCase):

    def test_uses_prompt_from_configs_table(self):
        Config.objects.create(
            name='assistant_system_prompt_blog',
            value='You are a custom blog advisor.',
            comment='test',
        )
        prompt = _get_system_prompt('blog')
        self.assertEqual(prompt, 'You are a custom blog advisor.')

    def test_falls_back_to_hardcoded_default(self):
        prompt = _get_system_prompt('blog')
        self.assertIn('technical blog', prompt.lower())
