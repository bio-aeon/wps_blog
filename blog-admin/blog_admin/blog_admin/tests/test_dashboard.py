from django.test import TestCase
from django.urls import reverse
from model_bakery import baker

from blog_admin.models import (
    User, Post, Tag, PostTag, Comment,
    Skill, Experience, SocialLink, Testimonial, ContactSubmission,
)


class DashboardViewTest(TestCase):
    def setUp(self):
        self.admin_user = User.objects.create_superuser('admin', 'a@b.com', 'pass')
        self.client.force_login(self.admin_user)

    def test_dashboard_loads(self):
        response = self.client.get(reverse('admin-dashboard'))
        self.assertEqual(response.status_code, 200)

    def test_dashboard_requires_staff(self):
        self.client.logout()
        response = self.client.get(reverse('admin-dashboard'))
        self.assertEqual(response.status_code, 302)

    def test_dashboard_shows_post_count(self):
        baker.make(Post, author=self.admin_user, _quantity=3)
        response = self.client.get(reverse('admin-dashboard'))
        self.assertContains(response, '3')

    def test_dashboard_shows_published_vs_draft(self):
        baker.make(Post, author=self.admin_user, is_hidden=False, _quantity=2)
        baker.make(Post, author=self.admin_user, is_hidden=True, _quantity=1)
        response = self.client.get(reverse('admin-dashboard'))
        self.assertContains(response, '2 published')
        self.assertContains(response, '1 drafts')

    def test_dashboard_shows_total_views(self):
        baker.make(Post, author=self.admin_user, views=100)
        baker.make(Post, author=self.admin_user, views=50)
        response = self.client.get(reverse('admin-dashboard'))
        self.assertContains(response, '150')

    def test_dashboard_shows_pending_comments(self):
        post = baker.make(Post, author=self.admin_user)
        Comment.objects.create(
            text='Test', name='A', email='a@b.com',
            post=post, is_approved=False
        )
        response = self.client.get(reverse('admin-dashboard'))
        self.assertContains(response, '1 pending moderation')

    def test_dashboard_shows_recent_comments(self):
        post = baker.make(Post, author=self.admin_user)
        Comment.objects.create(
            text='Great article!', name='Reader', email='r@b.com', post=post
        )
        response = self.client.get(reverse('admin-dashboard'))
        self.assertContains(response, 'Reader')

    def test_dashboard_shows_top_posts(self):
        baker.make(
            Post, author=self.admin_user, name='Popular Post',
            views=1000, is_hidden=False
        )
        response = self.client.get(reverse('admin-dashboard'))
        self.assertContains(response, 'Popular Post')

    def test_dashboard_shows_popular_tags(self):
        tag = baker.make(Tag, name='Rust', slug='rust')
        post = baker.make(Post, author=self.admin_user)
        PostTag.objects.create(post=post, tag=tag)
        response = self.client.get(reverse('admin-dashboard'))
        self.assertContains(response, 'Rust')

    def test_dashboard_shows_skill_count(self):
        Skill.objects.create(name='Rust', slug='rust', category='Languages', proficiency=80)
        Skill.objects.create(name='Python', slug='python', category='Languages', proficiency=90)
        Skill.objects.create(name='Old', slug='old', category='Legacy', is_active=False)
        response = self.client.get(reverse('admin-dashboard'))
        context = response.context
        self.assertEqual(context['skill_count'], 2)

    def test_dashboard_shows_experience_count(self):
        from datetime import date
        Experience.objects.create(
            company='Acme', position='Engineer', description='Work',
            start_date=date(2020, 1, 1)
        )
        response = self.client.get(reverse('admin-dashboard'))
        self.assertEqual(response.context['experience_count'], 1)

    def test_dashboard_shows_social_link_count(self):
        SocialLink.objects.create(platform='github', url='https://github.com/user')
        response = self.client.get(reverse('admin-dashboard'))
        self.assertEqual(response.context['social_link_count'], 1)

    def test_dashboard_shows_testimonial_count(self):
        Testimonial.objects.create(author_name='Alice', quote='Great')
        response = self.client.get(reverse('admin-dashboard'))
        self.assertEqual(response.context['testimonial_count'], 1)

    def test_dashboard_shows_unread_contacts(self):
        ContactSubmission.objects.create(
            name='Alice', email='a@b.com', subject='Hello',
            message='Hi there', is_read=False
        )
        ContactSubmission.objects.create(
            name='Bob', email='b@b.com', subject='Question',
            message='Got a question', is_read=True
        )
        response = self.client.get(reverse('admin-dashboard'))
        self.assertEqual(response.context['unread_contacts'], 1)

    def test_dashboard_shows_recent_contacts(self):
        ContactSubmission.objects.create(
            name='Alice', email='a@b.com', subject='Hello',
            message='Hi there'
        )
        response = self.client.get(reverse('admin-dashboard'))
        self.assertContains(response, 'Alice')

    def test_dashboard_handles_empty_state(self):
        response = self.client.get(reverse('admin-dashboard'))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, 'No comments yet.')


class AnalyticsViewTest(TestCase):
    def setUp(self):
        self.admin_user = User.objects.create_superuser('admin', 'a@b.com', 'pass')
        self.client.force_login(self.admin_user)

    def test_analytics_loads(self):
        response = self.client.get(reverse('admin-analytics'))
        self.assertEqual(response.status_code, 200)

    def test_analytics_requires_staff(self):
        self.client.logout()
        response = self.client.get(reverse('admin-analytics'))
        self.assertEqual(response.status_code, 302)

    def test_analytics_contains_chart_containers(self):
        response = self.client.get(reverse('admin-analytics'))
        self.assertContains(response, 'postsChart')
        self.assertContains(response, 'commentsChart')
        self.assertContains(response, 'viewsChart')

    def test_analytics_includes_chartjs(self):
        response = self.client.get(reverse('admin-analytics'))
        self.assertContains(response, 'chart.umd.min.js')

    def test_analytics_passes_json_data(self):
        baker.make(Post, author=self.admin_user, name='Test Post', views=42, is_hidden=False)
        response = self.client.get(reverse('admin-analytics'))
        self.assertContains(response, 'posts-data')
        self.assertContains(response, 'views-data')
        self.assertContains(response, 'comments-data')

    def test_analytics_handles_empty_data(self):
        response = self.client.get(reverse('admin-analytics'))
        self.assertEqual(response.status_code, 200)
