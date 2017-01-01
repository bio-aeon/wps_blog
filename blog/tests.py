# -*- coding: utf-8 -*-

from django.test import TestCase
from django.core.urlresolvers import reverse


class PostsViewTests(TestCase):
    def test_start(self):
        response = self.client.get(reverse('posts-start'))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, u'Блог | Web and programming solutions')

    def test_index(self):
        response = self.client.get(reverse('posts-index'))
        self.assertEqual(response.status_code, 200)
