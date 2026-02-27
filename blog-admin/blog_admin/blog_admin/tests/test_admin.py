from django.contrib import admin
from django.test import TestCase
from django.urls import reverse
from model_bakery import baker

from blog_admin.admin import PostAdmin, CommentAdmin, ExperienceAdmin
from blog_admin.models import (
    User, Post, Tag, PostTag, Comment, Page,
    Skill, Experience, SocialLink, ContactSubmission,
)


class UserAdminTest(TestCase):
    def setUp(self):
        self.admin_user = User.objects.create_superuser('admin', 'a@b.com', 'pass')
        self.client.force_login(self.admin_user)

    def test_user_changelist_loads(self):
        response = self.client.get(reverse('admin:blog_admin_user_changelist'))
        self.assertEqual(response.status_code, 200)

    def test_user_add_permission_denied(self):
        response = self.client.get(reverse('admin:blog_admin_user_add'))
        self.assertEqual(response.status_code, 403)

    def test_user_search_works(self):
        response = self.client.get(
            reverse('admin:blog_admin_user_changelist') + '?q=admin'
        )
        self.assertEqual(response.status_code, 200)


class PostAdminTest(TestCase):
    def setUp(self):
        self.admin_user = User.objects.create_superuser('admin', 'a@b.com', 'pass')
        self.client.force_login(self.admin_user)
        self.post = baker.make(Post, author=self.admin_user, name='Test Post')

    def test_post_changelist_loads(self):
        response = self.client.get(reverse('admin:blog_admin_post_changelist'))
        self.assertEqual(response.status_code, 200)

    def test_post_change_form_loads(self):
        response = self.client.get(
            reverse('admin:blog_admin_post_change', args=[self.post.pk])
        )
        self.assertEqual(response.status_code, 200)

    def test_publication_status_shows_draft_for_hidden(self):
        self.post.is_hidden = True
        self.post.save()
        admin_instance = PostAdmin(Post, admin.site)
        result = admin_instance.publication_status(self.post)
        self.assertIn('Draft', result)

    def test_publication_status_shows_published(self):
        self.post.is_hidden = False
        self.post.save()
        admin_instance = PostAdmin(Post, admin.site)
        result = admin_instance.publication_status(self.post)
        self.assertIn('Published', result)

    def test_tag_list_display(self):
        tag = baker.make(Tag, name='Rust', slug='rust')
        PostTag.objects.create(post=self.post, tag=tag)
        admin_instance = PostAdmin(Post, admin.site)
        post = Post.objects.prefetch_related('tags').get(pk=self.post.pk)
        self.assertEqual(admin_instance.tag_list(post), 'Rust')


class TagAdminTest(TestCase):
    def setUp(self):
        self.admin_user = User.objects.create_superuser('admin', 'a@b.com', 'pass')
        self.client.force_login(self.admin_user)

    def test_tag_changelist_loads(self):
        response = self.client.get(reverse('admin:blog_admin_tag_changelist'))
        self.assertEqual(response.status_code, 200)

    def test_tag_add_form_loads(self):
        response = self.client.get(reverse('admin:blog_admin_tag_add'))
        self.assertEqual(response.status_code, 200)

    def test_tag_add_creates_tag(self):
        response = self.client.post(reverse('admin:blog_admin_tag_add'), {
            'name': 'Scala',
            'slug': 'scala',
        })
        self.assertEqual(response.status_code, 302)
        self.assertTrue(Tag.objects.filter(slug='scala').exists())

    def test_post_count_annotation(self):
        tag = baker.make(Tag, name='Rust', slug='rust')
        post = baker.make(Post, author=self.admin_user)
        PostTag.objects.create(post=post, tag=tag)
        response = self.client.get(reverse('admin:blog_admin_tag_changelist'))
        self.assertContains(response, '1')


class PageAdminTest(TestCase):
    def setUp(self):
        self.admin_user = User.objects.create_superuser('admin', 'a@b.com', 'pass')
        self.client.force_login(self.admin_user)

    def test_page_changelist_loads(self):
        response = self.client.get(reverse('admin:blog_admin_page_changelist'))
        self.assertEqual(response.status_code, 200)

    def test_page_add_creates_page(self):
        response = self.client.post(reverse('admin:blog_admin_page_add'), {
            'url': 'about',
            'title': 'About Me',
            'content': '# About\n\nHello world.',
        })
        self.assertEqual(response.status_code, 302)
        self.assertTrue(Page.objects.filter(url='about').exists())


class CommentAdminTest(TestCase):
    def setUp(self):
        self.admin_user = User.objects.create_superuser('admin', 'a@b.com', 'pass')
        self.client.force_login(self.admin_user)
        self.post = baker.make(Post, author=self.admin_user)
        self.comment = Comment.objects.create(
            text='Great post!', name='Alice', email='alice@example.com',
            post=self.post, is_approved=False
        )

    def test_comment_changelist_loads(self):
        response = self.client.get(reverse('admin:blog_admin_comment_changelist'))
        self.assertEqual(response.status_code, 200)

    def test_short_text_display_truncates(self):
        long_comment = Comment(text='x' * 100, name='Test', email='t@t.com', post=self.post)
        admin_instance = CommentAdmin(Comment, admin.site)
        display = admin_instance.short_text_display(long_comment)
        self.assertEqual(len(display), 83)  # 80 chars + '...'
        self.assertTrue(display.endswith('...'))

    def test_short_text_display_no_truncation(self):
        short_comment = Comment(text='Short', name='Test', email='t@t.com', post=self.post)
        admin_instance = CommentAdmin(Comment, admin.site)
        self.assertEqual(admin_instance.short_text_display(short_comment), 'Short')


class ConfigAdminTest(TestCase):
    def setUp(self):
        self.admin_user = User.objects.create_superuser('admin', 'a@b.com', 'pass')
        self.client.force_login(self.admin_user)

    def test_config_changelist_loads(self):
        response = self.client.get(reverse('admin:blog_admin_config_changelist'))
        self.assertEqual(response.status_code, 200)


class SkillAdminTest(TestCase):
    def setUp(self):
        self.admin_user = User.objects.create_superuser('admin', 'a@b.com', 'pass')
        self.client.force_login(self.admin_user)

    def test_skill_changelist_loads(self):
        response = self.client.get(reverse('admin:blog_admin_skill_changelist'))
        self.assertEqual(response.status_code, 200)

    def test_skill_add_creates_skill(self):
        response = self.client.post(reverse('admin:blog_admin_skill_add'), {
            'name': 'Rust',
            'slug': 'rust',
            'category': 'Languages',
            'proficiency': 85,
            'sort_order': 0,
            'is_active': True,
        })
        self.assertEqual(response.status_code, 302)
        self.assertTrue(Skill.objects.filter(slug='rust').exists())


class ExperienceAdminTest(TestCase):
    def setUp(self):
        self.admin_user = User.objects.create_superuser('admin', 'a@b.com', 'pass')
        self.client.force_login(self.admin_user)

    def test_experience_changelist_loads(self):
        response = self.client.get(reverse('admin:blog_admin_experience_changelist'))
        self.assertEqual(response.status_code, 200)

    def test_is_current_true_when_no_end_date(self):
        from datetime import date
        exp = Experience(
            company='Acme', position='Engineer', description='Work',
            start_date=date(2020, 1, 1), end_date=None,
        )
        admin_instance = ExperienceAdmin(Experience, admin.site)
        self.assertTrue(admin_instance.is_current(exp))

    def test_is_current_false_when_end_date_set(self):
        from datetime import date
        exp = Experience(
            company='Acme', position='Engineer', description='Work',
            start_date=date(2020, 1, 1), end_date=date(2023, 1, 1),
        )
        admin_instance = ExperienceAdmin(Experience, admin.site)
        self.assertFalse(admin_instance.is_current(exp))


class SocialLinkAdminTest(TestCase):
    def setUp(self):
        self.admin_user = User.objects.create_superuser('admin', 'a@b.com', 'pass')
        self.client.force_login(self.admin_user)

    def test_sociallink_changelist_loads(self):
        response = self.client.get(reverse('admin:blog_admin_sociallink_changelist'))
        self.assertEqual(response.status_code, 200)


class ContactSubmissionAdminTest(TestCase):
    def setUp(self):
        self.admin_user = User.objects.create_superuser('admin', 'a@b.com', 'pass')
        self.client.force_login(self.admin_user)

    def test_contactsubmission_changelist_loads(self):
        response = self.client.get(reverse('admin:blog_admin_contactsubmission_changelist'))
        self.assertEqual(response.status_code, 200)

    def test_contactsubmission_add_permission_denied(self):
        response = self.client.get(reverse('admin:blog_admin_contactsubmission_add'))
        self.assertEqual(response.status_code, 403)

