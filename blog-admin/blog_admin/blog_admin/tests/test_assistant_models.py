from django.test import TestCase
from model_bakery import baker

from blog_admin.models import (
    ContentDraft, ContentTemplate, GenerationHistory, Language,
)


class ContentDraftModelTest(TestCase):

    def test_displays_platform_title_and_status(self):
        draft = ContentDraft(platform='blog', title='My Post', status='draft')
        self.assertEqual(str(draft), '[blog] My Post (draft)')

    def test_displays_untitled_when_title_is_none(self):
        draft = ContentDraft(platform='linkedin', title=None, status='idea')
        self.assertEqual(str(draft), '[linkedin] (untitled) (idea)')

    def test_defaults_to_draft_status(self):
        draft = ContentDraft()
        self.assertEqual(draft.status, 'draft')

    def test_orders_by_updated_at_descending(self):
        self.assertEqual(ContentDraft._meta.ordering, ['-updated_at'])

    def test_defaults_metadata_to_empty_dict(self):
        lang = baker.make(Language, code='en', name='English')
        draft = ContentDraft.objects.create(
            platform='blog', body='test', language=lang,
        )
        self.assertEqual(draft.metadata, {})

    def test_allows_null_source_post(self):
        lang = baker.make(Language, code='en', name='English')
        draft = ContentDraft.objects.create(
            platform='twitter', body='test', language=lang,
        )
        self.assertIsNone(draft.source_post)

    def test_unmanaged(self):
        self.assertFalse(ContentDraft._meta.managed)


class ContentTemplateModelTest(TestCase):

    def test_displays_platform_and_name(self):
        template = ContentTemplate(
            name='Tech Blog', platform='blog', prompt_template='...',
        )
        self.assertEqual(str(template), '[blog] Tech Blog')

    def test_orders_by_platform_then_name(self):
        self.assertEqual(ContentTemplate._meta.ordering, ['platform', 'name'])

    def test_unmanaged(self):
        self.assertFalse(ContentTemplate._meta.managed)


class GenerationHistoryModelTest(TestCase):

    def test_displays_id_model_and_draft(self):
        history = GenerationHistory(
            pk=7, draft_id=3, model='claude-sonnet-4-6',
            prompt='...', response='...',
        )
        self.assertEqual(
            str(history), 'Generation 7 (claude-sonnet-4-6) for draft 3'
        )

    def test_parses_input_tokens_from_usage(self):
        history = GenerationHistory(
            token_usage={'input': 150, 'output': 300},
        )
        self.assertEqual(history.input_tokens, 150)

    def test_parses_output_tokens_from_usage(self):
        history = GenerationHistory(
            token_usage={'input': 150, 'output': 300},
        )
        self.assertEqual(history.output_tokens, 300)

    def test_returns_zero_tokens_when_usage_is_null(self):
        history = GenerationHistory(token_usage=None)
        self.assertEqual(history.input_tokens, 0)
        self.assertEqual(history.output_tokens, 0)

    def test_deletes_history_when_draft_deleted(self):
        lang = baker.make(Language, code='en', name='English')
        draft = ContentDraft.objects.create(
            platform='blog', body='test', language=lang,
        )
        GenerationHistory.objects.create(
            draft=draft, prompt='p', model='m', response='r',
        )
        self.assertEqual(GenerationHistory.objects.count(), 1)
        draft.delete()
        self.assertEqual(GenerationHistory.objects.count(), 0)

    def test_unmanaged(self):
        self.assertFalse(GenerationHistory._meta.managed)
