# -*- coding: utf-8 -*-
from django.db import models
from django.core.mail import send_mail
from django.utils import timezone

from mptt.models import MPTTModel, TreeForeignKey
from .postmodel import Post


class Comment(MPTTModel):
    text = models.TextField()
    create_time = models.DateTimeField()
    name = models.CharField(max_length=255)
    email = models.EmailField()
    rating = models.IntegerField(default=0)
    post = models.ForeignKey(Post, related_name='comments')
    parent = TreeForeignKey('self', null=True, blank=True, related_name='children')

    class Meta:
        app_label = 'blog'
        verbose_name = u'Комментарий'
        verbose_name_plural = u'Комментарии'

    def save(self, *args, **kwargs):
        models.signals.post_save.connect(self.after_save,
                                         sender=Comment,
                                         dispatch_uid='comment_after_save_signal')
        super(Comment, self).save(*args, **kwargs)

    def after_save(self, **kwargs):
        pass
#        send_mail('Subject',
#                  'Here is the message.',
#                  'from@example.com',
#                  ['netstream.in@gmail.com'],
#                  fail_silently=True)

    def days_ago(self):
        return (timezone.now() - self.create_time).days


class CommentRate(models.Model):
    ip = models.GenericIPAddressField()
    comment = models.ForeignKey(Comment)

    class Meta:
        app_label = 'blog'
        db_table = 'blog_comment_rate'