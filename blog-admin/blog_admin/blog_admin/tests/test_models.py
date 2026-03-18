from django.apps import apps
from django.test import TestCase
from model_bakery import baker

from blog_admin.models import (
    User, Post, Tag, PostTag, Comment, CommentRater, Page, Config,
    Skill, Experience, SocialLink, ContactSubmission,
    Language, PostTranslation, PageTranslation, TagTranslation,
)


class TagModelTest(TestCase):
    def test_str_returns_name(self):
        tag = Tag(name='Rust', slug='rust')
        self.assertEqual(str(tag), 'Rust')


class PostTagModelTest(TestCase):
    def test_str_shows_post_and_tag(self):
        user = User.objects.create_superuser('admin', 'a@b.com', 'pass')
        post = Post.objects.create(
            name='Test', short_text='Short', text='Full text',
            author=user, meta_title='t', meta_keywords='k', meta_description='d',
        )
        tag = Tag.objects.create(name='Rust', slug='rust')
        pt = PostTag.objects.create(post=post, tag=tag)
        self.assertEqual(str(pt), 'Test - Rust')

    def test_m2m_through_relationship(self):
        """Post.tags M2M works through PostTag junction model."""
        user = User.objects.create_superuser('admin', 'a@b.com', 'pass')
        post = Post.objects.create(
            name='Test', short_text='Short', text='Full text',
            author=user, meta_title='t', meta_keywords='k', meta_description='d',
        )
        tag = Tag.objects.create(name='Scala', slug='scala')
        PostTag.objects.create(post=post, tag=tag)
        self.assertEqual(list(post.tags.all()), [tag])


class CommentModelTest(TestCase):
    def test_str_includes_author_and_post(self):
        user = User.objects.create_superuser('admin', 'a@b.com', 'pass')
        post = Post.objects.create(
            name='My Post', short_text='Short', text='Full text',
            author=user, meta_title='t', meta_keywords='k', meta_description='d',
        )
        comment = Comment(text='Great!', name='Alice', email='alice@test.com', post=post)
        self.assertEqual(str(comment), 'Comment by Alice on My Post')


class CommentRaterModelTest(TestCase):
    def test_str_representation(self):
        rater = CommentRater(ip='192.168.1.1', comment_id=42)
        self.assertEqual(str(rater), '192.168.1.1 on comment #42')


class PageModelTest(TestCase):
    def test_str_returns_title(self):
        page = Page(url='about', title='About Me', content='...')
        self.assertEqual(str(page), 'About Me')


class ConfigModelTest(TestCase):
    def test_str_format(self):
        config = Config(name='site_title', value='My Blog', comment='Title')
        self.assertEqual(str(config), 'site_title = My Blog')


class SkillModelTest(TestCase):
    def test_str_shows_name_and_category(self):
        skill = Skill(name='Rust', slug='rust', category='Languages')
        self.assertEqual(str(skill), 'Rust (Languages)')


class ExperienceModelTest(TestCase):
    def test_str_with_end_date(self):
        from datetime import date
        exp = Experience(
            position='Engineer', company='Acme',
            start_date=date(2020, 1, 1), end_date=date(2023, 6, 30),
            description='Built things'
        )
        self.assertEqual(str(exp), 'Engineer at Acme (2020-01-01 – 2023-06-30)')

    def test_str_without_end_date(self):
        from datetime import date
        exp = Experience(
            position='Engineer', company='Acme',
            start_date=date(2020, 1, 1), end_date=None,
            description='Building things'
        )
        self.assertEqual(str(exp), 'Engineer at Acme (2020-01-01 – Present)')


class SocialLinkModelTest(TestCase):
    def test_str_with_label(self):
        link = SocialLink(platform='github', url='https://github.com/user', label='GitHub')
        self.assertEqual(str(link), 'github: GitHub')

    def test_str_without_label(self):
        link = SocialLink(platform='github', url='https://github.com/user')
        self.assertEqual(str(link), 'github: https://github.com/user')


class ContactSubmissionModelTest(TestCase):
    def test_str_shows_subject_and_name(self):
        submission = ContactSubmission(
            name='Alice', email='alice@test.com',
            subject='Hello', message='Hi there'
        )
        self.assertEqual(str(submission), 'Hello (from Alice)')



class LanguageModelTest(TestCase):
    def test_str_representation(self):
        lang = Language(code='ru', name='Russian', native_name='Русский')
        self.assertEqual(str(lang), 'Русский (ru)')

    def test_ordering(self):
        self.assertEqual(Language._meta.ordering, ['sort_order'])


class PostTranslationModelTest(TestCase):
    def test_str_representation(self):
        user = baker.make('blog_admin.User')
        post = baker.make(Post, author=user)
        lang = Language.objects.create(
            code='en', name='English', native_name='English',
            is_default=True, sort_order=1
        )
        translation = PostTranslation.objects.create(
            post=post, language=lang, name='Test Post',
            translation_status='draft'
        )
        self.assertIn('[en]', str(translation))
        self.assertIn('Test Post', str(translation))

    def test_unique_together_constraint(self):
        user = baker.make('blog_admin.User')
        post = baker.make(Post, author=user)
        lang = Language.objects.create(
            code='en', name='English', native_name='English',
            is_default=True, sort_order=1
        )
        PostTranslation.objects.create(
            post=post, language=lang, name='First',
            translation_status='draft'
        )
        from django.db import IntegrityError
        with self.assertRaises(IntegrityError):
            PostTranslation.objects.create(
                post=post, language=lang, name='Duplicate',
                translation_status='draft'
            )


class PageTranslationModelTest(TestCase):
    def test_str_representation(self):
        page = baker.make(Page)
        lang = Language.objects.create(
            code='en', name='English', native_name='English',
            is_default=True, sort_order=1
        )
        translation = PageTranslation.objects.create(
            page=page, language=lang, title='About',
            translation_status='draft'
        )
        self.assertIn('[en]', str(translation))
        self.assertIn('About', str(translation))


class TagTranslationModelTest(TestCase):
    def test_str_representation(self):
        tag = baker.make(Tag)
        lang = Language.objects.create(
            code='en', name='English', native_name='English',
            is_default=True, sort_order=1
        )
        translation = TagTranslation.objects.create(
            tag=tag, language=lang, name='Rust'
        )
        self.assertIn('[en]', str(translation))
        self.assertIn('Rust', str(translation))


class TranslationModelsUnmanagedTest(TestCase):
    def test_all_translation_models_unmanaged(self):
        for model in [Language, PostTranslation, PageTranslation, TagTranslation]:
            self.assertFalse(
                model._meta.managed,
                f"{model.__name__} must have managed=False"
            )


class AllModelsUnmanagedTest(TestCase):
    """Verify all models are unmanaged (schema owned by Flyway)."""

    def test_all_models_unmanaged(self):
        for model in apps.get_app_config('blog_admin').get_models():
            self.assertFalse(
                model._meta.managed,
                f"{model.__name__} should have managed=False"
            )
