# -*- coding: utf-8 -*-
from django.db import models
from taggit.managers import TaggableManager
from wpssite.blog.models.tagmodel import BlogTaggedItem, TaggedManager
from django.contrib.auth.models import User


class Post(models.Model):
    name = models.CharField(max_length=255, verbose_name=u'Наименование')
    short_text = models.TextField(verbose_name=u'Краткое описание')
    text = models.TextField(verbose_name=u'Текст')
    create_time = models.DateTimeField(verbose_name=u'Время создания')
    views = models.IntegerField(verbose_name=u'Просмотры', default=0)
    hidden = models.BooleanField(verbose_name=u'Скрыт', default=True)
    meta_title = models.CharField(max_length=255, null=True, blank=True)
    meta_keywords = models.CharField(max_length=255, null=True, blank=True)
    meta_description = models.CharField(max_length=255, null=True, blank=True)
    author = models.ForeignKey(User, verbose_name=u'Автор')
    tags = TaggableManager(verbose_name=u'Теги', through=BlogTaggedItem)
    tagged = TaggedManager()
    objects = models.Manager()

    class Meta:
        app_label = 'blog'
        verbose_name = u'Пост'
        verbose_name_plural = u'Посты'

    def __unicode__(self):
        return self.name