# -*- coding: utf-8 -*-
from wpssite.views import ClassView
from wpssite.blog.models import Post, Comment, CommentRate
from wpssite.blog.models.tagmodel import BlogTag
from wpssite.blog.forms import CommentForm
from django.db import models
from django.http import HttpResponse, Http404
from django.utils import simplejson
from django.template.loader import render_to_string
from django.core.paginator import Paginator, EmptyPage
from django.core.urlresolvers import reverse
import socket


class Posts(ClassView):
    def index(self):
        post_list = Post.tagged.order_by('-create_time')
        paginator = Paginator(post_list, 10)
        page = int(self._request.GET.get('page', 1))
        try:
            posts = paginator.page(page)
        except EmptyPage:
            posts = paginator.page(paginator.num_pages)
        return self._render('posts/list.html', {'posts': posts})

    def view(self, id):
        try:
            post = Post.objects.get(pk=id)
            if not self._request.session.get('post-' + id, False):
                post.views = models.F('views') + 1
                post.save()
            self._request.session['post-' + id] = True
        except Post.DoesNotExist:
            raise Http404
        return self._render('posts/view.html',
                            {'post': post,
                             'absolute_url': self._request.build_absolute_uri(reverse('posts-view',
                                                                                      args=[post.id]))})

    def start(self):
        Exception(socket.gethostbyname(socket.gethostname()))
        posts = Post.tagged.all().order_by('-create_time')[:10]
        # return self._render('posts/start.html', {'posts': posts})

    def search(self):
        search_text = unicode(self._request.GET.get('q', ''))
        if search_text:
            post_list = Post.objects.filter(models.Q(text__icontains=search_text) |
                                        models.Q(name__icontains=search_text))\
                                .prefetch_related('tagged_items__tag')\
                                .order_by('-create_time')
        else:
            post_list = Post.objects.none()

        paginator = Paginator(post_list, 10)
        page = int(self._request.GET.get('page', 1))
        try:
            posts = paginator.page(page)
        except EmptyPage:
            posts = paginator.page(paginator.num_pages)
        return self._render('posts/search.html', {'posts': posts, 'search_text': search_text})

    def tag(self, tag_slug):
        try:
            tag = BlogTag.objects.get(slug=tag_slug)
        except BlogTag.DoesNotExist:
            raise Http404

        post_list = Post.tagged.all()\
                               .filter(tags__slug=tag.slug)\
                               .order_by('-create_time')
        paginator = Paginator(post_list, 10)
        page = int(self._request.GET.get('page', 1))
        try:
            posts = paginator.page(page)
        except EmptyPage:
            posts = paginator.page(paginator.num_pages)
        return self._render('posts/tag.html', {'posts': posts, 'tag_name': tag.name})


class Comments(ClassView):
    def create(self):
        if self._request.is_ajax() and self._request.method == 'POST':
            form = CommentForm(self._request.POST)
            if form.is_valid():
                from django.utils import timezone
                comment = form.save(commit=False)
                comment.create_time = timezone.now()
                comment.save()

                return HttpResponse(simplejson.dumps({'data': render_to_string('comments/comment.html',
                                                                              {'node': comment}),
                                                      'id': comment.id,
                                                      'result': 'success'}))
            else:
                errors = dict((k, form.errors[k][0]) for k in form.errors)
                return HttpResponse(simplejson.dumps({'data': errors, 'result': 'error'}))
        else:
            raise Http404

    def rate(self, id, to):
        if self._request.is_ajax():
            ip = ip=self._request.META.get('REMOTE_ADDR', None)
            try:
                CommentRate.objects.get(comment=id, ip=self._request.META.get('REMOTE_ADDR', None))
            except CommentRate.DoesNotExist:
                comment = self._load_model(Comment, id)
                if to == 'up':
                    comment.rating = models.F('rating') + 1
                elif to == 'down':
                    comment.rating = models.F('rating') - 1
                comment.save()
                CommentRate(comment=comment, ip=ip).save()
                return HttpResponse(simplejson.dumps({'rating': self._load_model(Comment, id).rating}))
            else:
                return HttpResponse(simplejson.dumps({'error': u'Вы уже голосовали за этот комментарий'}))
        else:
            raise Http404