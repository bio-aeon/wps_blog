from django.test import TestCase
from django.urls import reverse
from model_bakery import baker

from blog_admin.models import User, Post, Comment


class CommentModerationTest(TestCase):
    def setUp(self):
        self.admin_user = User.objects.create_superuser('admin', 'a@b.com', 'pass')
        self.client.force_login(self.admin_user)
        self.post = baker.make(Post, author=self.admin_user)
        self.comment1 = Comment.objects.create(
            text='Great post!', name='Alice', email='alice@example.com',
            post=self.post, is_approved=False
        )
        self.comment2 = Comment.objects.create(
            text='Thanks!', name='Bob', email='bob@example.com',
            post=self.post, is_approved=False
        )

    def test_approve_action(self):
        response = self.client.post(
            reverse('admin:blog_admin_comment_changelist'),
            {
                'action': 'approve_comments',
                '_selected_action': [self.comment1.pk, self.comment2.pk],
            }
        )
        self.assertEqual(response.status_code, 302)
        self.comment1.refresh_from_db()
        self.comment2.refresh_from_db()
        self.assertTrue(self.comment1.is_approved)
        self.assertTrue(self.comment2.is_approved)

    def test_reject_action(self):
        self.comment1.is_approved = True
        self.comment1.save()
        response = self.client.post(
            reverse('admin:blog_admin_comment_changelist'),
            {
                'action': 'reject_comments',
                '_selected_action': [self.comment1.pk],
            }
        )
        self.assertEqual(response.status_code, 302)
        self.comment1.refresh_from_db()
        self.assertFalse(self.comment1.is_approved)

    def test_comment_changelist_filters(self):
        response = self.client.get(
            reverse('admin:blog_admin_comment_changelist') + '?is_approved__exact=0'
        )
        self.assertEqual(response.status_code, 200)

    def test_comment_search(self):
        response = self.client.get(
            reverse('admin:blog_admin_comment_changelist') + '?q=Alice'
        )
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, 'Alice')

    def test_comment_delete(self):
        pk = self.comment1.pk
        response = self.client.post(
            reverse('admin:blog_admin_comment_delete', args=[pk]),
            {'post': 'yes'}
        )
        self.assertEqual(response.status_code, 302)
        self.assertFalse(Comment.objects.filter(pk=pk).exists())

    def test_nested_comment_delete_cascades(self):
        reply = Comment.objects.create(
            text='Reply', name='Carol', email='carol@example.com',
            post=self.post, parent=self.comment1
        )
        self.client.post(
            reverse('admin:blog_admin_comment_delete', args=[self.comment1.pk]),
            {'post': 'yes'}
        )
        self.assertFalse(Comment.objects.filter(pk=reply.pk).exists())


class PublishWorkflowTest(TestCase):
    def setUp(self):
        self.admin_user = User.objects.create_superuser('admin', 'a@b.com', 'pass')
        self.client.force_login(self.admin_user)
        self.draft_post = baker.make(Post, author=self.admin_user, is_hidden=True)
        self.published_post = baker.make(Post, author=self.admin_user, is_hidden=False)

    def test_new_post_defaults_to_draft(self):
        self.assertTrue(Post._meta.get_field('is_hidden').default)

    def test_publish_action_sets_is_hidden_false(self):
        self.client.post(
            reverse('admin:blog_admin_post_changelist'),
            {
                'action': 'publish_posts',
                '_selected_action': [self.draft_post.pk],
            }
        )
        self.draft_post.refresh_from_db()
        self.assertFalse(self.draft_post.is_hidden)

    def test_unpublish_action_sets_is_hidden_true(self):
        self.client.post(
            reverse('admin:blog_admin_post_changelist'),
            {
                'action': 'unpublish_posts',
                '_selected_action': [self.published_post.pk],
            }
        )
        self.published_post.refresh_from_db()
        self.assertTrue(self.published_post.is_hidden)

    def test_bulk_publish_multiple_posts(self):
        draft2 = baker.make(Post, author=self.admin_user, is_hidden=True)
        self.client.post(
            reverse('admin:blog_admin_post_changelist'),
            {
                'action': 'publish_posts',
                '_selected_action': [self.draft_post.pk, draft2.pk],
            }
        )
        self.draft_post.refresh_from_db()
        draft2.refresh_from_db()
        self.assertFalse(self.draft_post.is_hidden)
        self.assertFalse(draft2.is_hidden)

    def test_filter_by_draft_status(self):
        response = self.client.get(
            reverse('admin:blog_admin_post_changelist') + '?is_hidden__exact=1'
        )
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, self.draft_post.name)
        self.assertNotContains(response, self.published_post.name)

    def test_filter_by_published_status(self):
        response = self.client.get(
            reverse('admin:blog_admin_post_changelist') + '?is_hidden__exact=0'
        )
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, self.published_post.name)
        self.assertNotContains(response, self.draft_post.name)
