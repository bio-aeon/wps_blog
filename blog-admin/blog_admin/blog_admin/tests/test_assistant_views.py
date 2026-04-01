import json
from unittest.mock import patch, MagicMock

from django.test import TestCase
from model_bakery import baker

from blog_admin.assistant.services.llm_client import LLMResponse
from blog_admin.models import (
    User, Post, Language, ContentDraft, ContentTemplate,
    GenerationHistory, PostTranslation,
)


class DraftViewTestBase(TestCase):
    """Shared setUp for draft view tests."""

    def setUp(self):
        self.admin_user = User.objects.create_superuser(
            'admin', 'a@b.com', 'testpass123'
        )
        self.client.force_login(self.admin_user)
        self.lang = baker.make(Language, code='en', name='English')

    def _post_json(self, url, data, **kwargs):
        return self.client.post(
            url, data=json.dumps(data),
            content_type='application/json', **kwargs,
        )

    def _put_json(self, url, data, **kwargs):
        return self.client.put(
            url, data=json.dumps(data),
            content_type='application/json', **kwargs,
        )


class DraftListCreateTest(DraftViewTestBase):

    def test_list_empty(self):
        response = self.client.get('/admin/assistant/drafts/')
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data['drafts'], [])
        self.assertEqual(data['total'], 0)

    def test_list_with_data(self):
        ContentDraft.objects.create(
            platform='blog', title='Post 1', body='body', language=self.lang,
        )
        ContentDraft.objects.create(
            platform='linkedin', title='Post 2', body='body', language=self.lang,
        )
        response = self.client.get('/admin/assistant/drafts/')
        data = response.json()
        self.assertEqual(data['total'], 2)
        self.assertEqual(len(data['drafts']), 2)

    def test_list_filter_by_platform(self):
        ContentDraft.objects.create(
            platform='blog', body='b', language=self.lang,
        )
        ContentDraft.objects.create(
            platform='linkedin', body='l', language=self.lang,
        )
        response = self.client.get('/admin/assistant/drafts/?platform=blog')
        data = response.json()
        self.assertEqual(data['total'], 1)
        self.assertEqual(data['drafts'][0]['platform'], 'blog')

    def test_list_filter_by_status(self):
        ContentDraft.objects.create(
            platform='blog', body='b', status='draft', language=self.lang,
        )
        ContentDraft.objects.create(
            platform='blog', body='b', status='approved', language=self.lang,
        )
        response = self.client.get('/admin/assistant/drafts/?status=approved')
        data = response.json()
        self.assertEqual(data['total'], 1)

    def test_create_draft(self):
        response = self._post_json('/admin/assistant/drafts/', {
            'platform': 'blog',
            'title': 'New Post',
            'body': 'Content here',
        })
        self.assertEqual(response.status_code, 201)
        data = response.json()
        self.assertEqual(data['platform'], 'blog')
        self.assertEqual(data['title'], 'New Post')
        self.assertEqual(data['status'], 'draft')
        self.assertEqual(ContentDraft.objects.count(), 1)

    def test_rejects_missing_platform(self):
        response = self._post_json('/admin/assistant/drafts/', {
            'body': 'no platform',
        })
        self.assertEqual(response.status_code, 400)

    def test_rejects_invalid_platform(self):
        response = self._post_json('/admin/assistant/drafts/', {
            'platform': 'facebook',
            'body': 'test',
        })
        self.assertEqual(response.status_code, 400)


class DraftDetailTest(DraftViewTestBase):

    def test_returns_draft_by_id(self):
        draft = ContentDraft.objects.create(
            platform='blog', title='Test', body='body', language=self.lang,
        )
        response = self.client.get(f'/admin/assistant/drafts/{draft.pk}/')
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data['title'], 'Test')
        self.assertEqual(data['platform'], 'blog')

    def test_returns_404_for_nonexistent_id(self):
        response = self.client.get('/admin/assistant/drafts/99999/')
        self.assertEqual(response.status_code, 404)

    def test_update_draft(self):
        draft = ContentDraft.objects.create(
            platform='blog', title='Old', body='old', language=self.lang,
        )
        response = self._put_json(f'/admin/assistant/drafts/{draft.pk}/', {
            'title': 'Updated',
            'body': 'new body',
            'status': 'review',
        })
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data['title'], 'Updated')
        self.assertEqual(data['body'], 'new body')
        self.assertEqual(data['status'], 'review')

    def test_delete_draft(self):
        draft = ContentDraft.objects.create(
            platform='blog', body='x', language=self.lang,
        )
        response = self.client.delete(f'/admin/assistant/drafts/{draft.pk}/')
        self.assertEqual(response.status_code, 204)
        self.assertEqual(ContentDraft.objects.count(), 0)


class DraftPublishTest(DraftViewTestBase):

    def test_publish_blog_draft(self):
        draft = ContentDraft.objects.create(
            platform='blog', title='My Post', body='Full content',
            status='approved', language=self.lang,
        )
        response = self._post_json(
            f'/admin/assistant/drafts/{draft.pk}/publish/', {},
        )
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertIn('post_id', data)

        post = Post.objects.get(pk=data['post_id'])
        self.assertEqual(post.name, 'My Post')
        self.assertFalse(post.is_hidden)

        translation = PostTranslation.objects.get(post=post)
        self.assertEqual(translation.language_id, 'en')
        self.assertEqual(translation.translation_status, 'published')

        draft.refresh_from_db()
        self.assertEqual(draft.status, 'published')
        self.assertEqual(draft.metadata['published_post_id'], post.pk)

    def test_publish_non_blog_rejected(self):
        draft = ContentDraft.objects.create(
            platform='linkedin', body='x', status='approved',
            language=self.lang,
        )
        response = self._post_json(
            f'/admin/assistant/drafts/{draft.pk}/publish/', {},
        )
        self.assertEqual(response.status_code, 400)

    def test_publish_non_approved_rejected(self):
        draft = ContentDraft.objects.create(
            platform='blog', body='x', status='draft', language=self.lang,
        )
        response = self._post_json(
            f'/admin/assistant/drafts/{draft.pk}/publish/', {},
        )
        self.assertEqual(response.status_code, 400)


class DraftAuthTest(TestCase):

    def test_drafts_require_auth(self):
        response = self.client.get('/admin/assistant/drafts/')
        self.assertIn(response.status_code, [301, 302])

    def test_draft_create_requires_auth(self):
        response = self.client.post(
            '/admin/assistant/drafts/',
            data='{}', content_type='application/json',
        )
        self.assertIn(response.status_code, [301, 302])

    def test_draft_publish_requires_auth(self):
        response = self.client.post(
            '/admin/assistant/drafts/1/publish/',
            data='{}', content_type='application/json',
        )
        self.assertIn(response.status_code, [301, 302])


# -- Generation endpoint tests -------------------------------------------------

class GenerateEndpointTest(DraftViewTestBase):

    @patch('blog_admin.assistant.services.generation.get_llm_client')
    def test_creates_blog_draft(self, mock_get_client):
        mock_client = MagicMock()
        mock_client.generate.return_value = LLMResponse(
            '# Post\nContent', 'claude-sonnet-4-6', 100, 200,
        )
        mock_get_client.return_value = mock_client

        response = self._post_json('/admin/assistant/generate/blog/', {
            'topic': 'Rust tips', 'language': 'en',
        })
        self.assertEqual(response.status_code, 201)
        data = response.json()
        self.assertEqual(data['platform'], 'blog')
        self.assertIn('generation', data)

    @patch('blog_admin.assistant.services.generation.get_llm_client')
    def test_creates_linkedin_draft(self, mock_get_client):
        mock_client = MagicMock()
        mock_client.generate.return_value = LLMResponse(
            'LinkedIn post\n\n#Scala', 'model', 50, 80,
        )
        mock_get_client.return_value = mock_client

        response = self._post_json('/admin/assistant/generate/linkedin/', {
            'topic': 'Scala tips', 'language': 'en',
        })
        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.json()['platform'], 'linkedin')

    @patch('blog_admin.assistant.services.generation.get_llm_client')
    def test_creates_twitter_draft(self, mock_get_client):
        mock_client = MagicMock()
        mock_client.generate_json.return_value = (
            {'tweet': 'Quick tip! #Rust'},
            LLMResponse('...', 'model', 30, 40),
        )
        mock_get_client.return_value = mock_client

        response = self._post_json('/admin/assistant/generate/twitter/', {
            'topic': 'Rust tip', 'format': 'single', 'language': 'en',
        })
        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.json()['platform'], 'twitter')

    def test_rejects_missing_topic(self):
        response = self._post_json('/admin/assistant/generate/blog/', {
            'language': 'en',
        })
        self.assertEqual(response.status_code, 400)
        self.assertIn('topic', response.json()['error'])

    def test_rejects_invalid_platform(self):
        response = self._post_json('/admin/assistant/generate/facebook/', {
            'topic': 'Test',
        })
        self.assertEqual(response.status_code, 400)

    def test_redirects_anonymous_user(self):
        self.client.logout()
        for platform in ('blog', 'linkedin', 'twitter'):
            response = self.client.post(
                f'/admin/assistant/generate/{platform}/',
                data='{}', content_type='application/json',
            )
            self.assertIn(response.status_code, [301, 302])


# -- Template CRUD tests -------------------------------------------------------

class TemplateListCreateTest(DraftViewTestBase):

    def test_list_empty(self):
        response = self.client.get('/admin/assistant/templates/')
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data['templates'], [])

    def test_list_with_data(self):
        ContentTemplate.objects.create(
            name='Tech Blog', platform='blog', prompt_template='Write about {topic}',
        )
        response = self.client.get('/admin/assistant/templates/')
        data = response.json()
        self.assertEqual(data['total'], 1)
        self.assertEqual(data['templates'][0]['name'], 'Tech Blog')

    def test_list_filter_by_platform(self):
        ContentTemplate.objects.create(
            name='Blog T', platform='blog', prompt_template='...',
        )
        ContentTemplate.objects.create(
            name='Tweet T', platform='twitter', prompt_template='...',
        )
        response = self.client.get('/admin/assistant/templates/?platform=twitter')
        data = response.json()
        self.assertEqual(data['total'], 1)
        self.assertEqual(data['templates'][0]['platform'], 'twitter')

    def test_create_template(self):
        response = self._post_json('/admin/assistant/templates/', {
            'name': 'LinkedIn Thought Leadership',
            'platform': 'linkedin',
            'prompt_template': 'Write a post about {topic}',
            'description': 'For professional posts',
        })
        self.assertEqual(response.status_code, 201)
        data = response.json()
        self.assertEqual(data['name'], 'LinkedIn Thought Leadership')
        self.assertEqual(ContentTemplate.objects.count(), 1)

    def test_rejects_missing_fields(self):
        response = self._post_json('/admin/assistant/templates/', {
            'name': 'Incomplete',
        })
        self.assertEqual(response.status_code, 400)

    def test_create_invalid_platform(self):
        response = self._post_json('/admin/assistant/templates/', {
            'name': 'Bad', 'platform': 'tiktok', 'prompt_template': '...',
        })
        self.assertEqual(response.status_code, 400)


class TemplateDetailTest(DraftViewTestBase):

    def test_returns_template_by_id(self):
        t = ContentTemplate.objects.create(
            name='Test', platform='blog', prompt_template='...',
        )
        response = self.client.get(f'/admin/assistant/templates/{t.pk}/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['name'], 'Test')

    def test_update_template(self):
        t = ContentTemplate.objects.create(
            name='Old', platform='blog', prompt_template='old',
        )
        response = self._put_json(f'/admin/assistant/templates/{t.pk}/', {
            'name': 'Updated',
            'prompt_template': 'new template',
        })
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['name'], 'Updated')

    def test_delete_template(self):
        t = ContentTemplate.objects.create(
            name='Delete Me', platform='blog', prompt_template='...',
        )
        response = self.client.delete(f'/admin/assistant/templates/{t.pk}/')
        self.assertEqual(response.status_code, 204)
        self.assertEqual(ContentTemplate.objects.count(), 0)

    def test_returns_404_for_nonexistent_id(self):
        response = self.client.get('/admin/assistant/templates/99999/')
        self.assertEqual(response.status_code, 404)


# -- History endpoint tests ----------------------------------------------------

class GenerationHistoryListTest(DraftViewTestBase):

    def test_list_empty(self):
        response = self.client.get('/admin/assistant/history/')
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data['history'], [])
        self.assertEqual(data['total'], 0)

    def test_list_with_data(self):
        draft = ContentDraft.objects.create(
            platform='blog', body='x', language=self.lang,
        )
        GenerationHistory.objects.create(
            draft=draft, prompt='test prompt', model='claude-sonnet-4-6',
            response='response text',
            token_usage={'input': 100, 'output': 200},
        )
        response = self.client.get('/admin/assistant/history/')
        data = response.json()
        self.assertEqual(data['total'], 1)
        self.assertEqual(data['history'][0]['model'], 'claude-sonnet-4-6')

    def test_filter_by_draft(self):
        d1 = ContentDraft.objects.create(
            platform='blog', body='x', language=self.lang,
        )
        d2 = ContentDraft.objects.create(
            platform='linkedin', body='y', language=self.lang,
        )
        GenerationHistory.objects.create(
            draft=d1, prompt='p', model='m', response='r',
        )
        GenerationHistory.objects.create(
            draft=d2, prompt='p', model='m', response='r',
        )
        response = self.client.get(f'/admin/assistant/history/?draft_id={d1.pk}')
        data = response.json()
        self.assertEqual(data['total'], 1)

    def test_prompt_truncated_in_list(self):
        draft = ContentDraft.objects.create(
            platform='blog', body='x', language=self.lang,
        )
        long_prompt = 'A' * 500
        GenerationHistory.objects.create(
            draft=draft, prompt=long_prompt, model='m', response='r',
        )
        response = self.client.get('/admin/assistant/history/')
        data = response.json()
        self.assertTrue(len(data['history'][0]['prompt']) <= 200)

    def test_redirects_anonymous_user(self):
        self.client.logout()
        response = self.client.get('/admin/assistant/history/')
        self.assertIn(response.status_code, [301, 302])

    def test_rejects_write_methods(self):
        response = self.client.post(
            '/admin/assistant/history/',
            data='{}', content_type='application/json',
        )
        self.assertEqual(response.status_code, 405)

        response = self.client.put(
            '/admin/assistant/history/',
            data='{}', content_type='application/json',
        )
        self.assertEqual(response.status_code, 405)

        response = self.client.delete('/admin/assistant/history/')
        self.assertEqual(response.status_code, 405)


# -- Dashboard view tests ------------------------------------------------------

class AssistantDashboardTest(DraftViewTestBase):

    def test_loads(self):
        response = self.client.get('/admin/assistant/')
        self.assertEqual(response.status_code, 200)
        self.assertTemplateUsed(response, 'admin/assistant/dashboard.html')

    def test_redirects_anonymous_user(self):
        self.client.logout()
        response = self.client.get('/admin/assistant/')
        self.assertIn(response.status_code, [301, 302])

    def test_shows_draft_counts_per_platform(self):
        ContentDraft.objects.create(
            platform='blog', body='x', language=self.lang,
        )
        ContentDraft.objects.create(
            platform='linkedin', body='y', language=self.lang,
        )
        response = self.client.get('/admin/assistant/')
        self.assertEqual(response.context['draft_counts']['blog'], 1)
        self.assertEqual(response.context['draft_counts']['linkedin'], 1)
        self.assertEqual(response.context['draft_counts']['twitter'], 0)

    def test_shows_recent_drafts(self):
        ContentDraft.objects.create(
            platform='blog', title='My Draft', body='x', language=self.lang,
        )
        response = self.client.get('/admin/assistant/')
        self.assertEqual(len(response.context['recent_drafts']), 1)

    def test_shows_monthly_token_usage(self):
        draft = ContentDraft.objects.create(
            platform='blog', body='x', language=self.lang,
        )
        GenerationHistory.objects.create(
            draft=draft, prompt='p', model='m', response='r',
            token_usage={'input': 100, 'output': 200},
        )
        response = self.client.get('/admin/assistant/')
        self.assertEqual(response.context['monthly_generations'], 1)

    def test_main_dashboard_has_assistant_link(self):
        response = self.client.get('/admin/dashboard/')
        self.assertContains(response, 'Open Assistant')
        self.assertIn('assistant_draft_count', response.context)

    def test_shows_estimated_cost(self):
        draft = ContentDraft.objects.create(
            platform='blog', body='x', language=self.lang,
        )
        GenerationHistory.objects.create(
            draft=draft, prompt='p', model='m', response='r',
            token_usage={'input': 1000, 'output': 500},
        )
        response = self.client.get('/admin/assistant/')
        self.assertIn('estimated_cost', response.context)
        # cost = 1000 * 0.000003 + 500 * 0.000015 = 0.003 + 0.0075 = 0.0105
        self.assertAlmostEqual(response.context['estimated_cost'], 0.0105, places=4)

    def test_warns_when_budget_threshold_exceeded(self):
        from blog_admin.models import Config
        Config.objects.create(
            name='assistant_monthly_token_budget', value='100', comment='test',
        )
        draft = ContentDraft.objects.create(
            platform='blog', body='x', language=self.lang,
        )
        GenerationHistory.objects.create(
            draft=draft, prompt='p', model='m', response='r',
            token_usage={'input': 50, 'output': 40},
        )
        response = self.client.get('/admin/assistant/')
        self.assertTrue(response.context['budget_warning'])
        self.assertContains(response, 'budget warning')

    def test_no_warning_under_budget_threshold(self):
        response = self.client.get('/admin/assistant/')
        self.assertFalse(response.context['budget_warning'])


# -- Composer view tests -------------------------------------------------------

class BlogComposerTest(DraftViewTestBase):

    def test_loads(self):
        response = self.client.get('/admin/assistant/compose/blog/')
        self.assertEqual(response.status_code, 200)
        self.assertTemplateUsed(response, 'admin/assistant/blog_composer.html')

    def test_requires_staff(self):
        self.client.logout()
        response = self.client.get('/admin/assistant/compose/blog/')
        self.assertIn(response.status_code, [301, 302])

    def test_loads_existing_draft(self):
        draft = ContentDraft.objects.create(
            platform='blog', title='My Draft', body='Content',
            language=self.lang,
        )
        response = self.client.get(
            f'/admin/assistant/compose/blog/?draft_id={draft.pk}'
        )
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.context['draft'].pk, draft.pk)

    def test_includes_composer_js(self):
        response = self.client.get('/admin/assistant/compose/blog/')
        self.assertContains(response, 'composer.js')


class LinkedInComposerTest(DraftViewTestBase):

    def test_loads(self):
        response = self.client.get('/admin/assistant/compose/linkedin/')
        self.assertEqual(response.status_code, 200)
        self.assertTemplateUsed(response, 'admin/assistant/linkedin_composer.html')

    def test_shows_char_counter(self):
        response = self.client.get('/admin/assistant/compose/linkedin/')
        self.assertContains(response, 'char-counter')

    def test_requires_staff(self):
        self.client.logout()
        response = self.client.get('/admin/assistant/compose/linkedin/')
        self.assertIn(response.status_code, [301, 302])


class TwitterComposerTest(DraftViewTestBase):

    def test_loads(self):
        response = self.client.get('/admin/assistant/compose/twitter/')
        self.assertEqual(response.status_code, 200)
        self.assertTemplateUsed(response, 'admin/assistant/twitter_composer.html')

    def test_shows_format_toggle(self):
        response = self.client.get('/admin/assistant/compose/twitter/')
        self.assertContains(response, 'format-select')

    def test_requires_staff(self):
        self.client.logout()
        response = self.client.get('/admin/assistant/compose/twitter/')
        self.assertIn(response.status_code, [301, 302])


# -- Repurpose endpoint tests --------------------------------------------------

class RepurposeEndpointTest(DraftViewTestBase):

    def test_redirects_anonymous_user(self):
        self.client.logout()
        response = self.client.post(
            '/admin/assistant/repurpose/',
            data='{}', content_type='application/json',
        )
        self.assertIn(response.status_code, [301, 302])

    def test_rejects_missing_fields(self):
        response = self._post_json('/admin/assistant/repurpose/', {})
        self.assertEqual(response.status_code, 400)
