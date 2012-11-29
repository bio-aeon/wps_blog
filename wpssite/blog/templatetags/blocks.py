# -*- coding: utf-8 -*-
from wpssite.blog.models.tagmodel import BlogTag
from wpssite.blog.models.postmodel import Post
from wpssite.blog.forms import CommentForm
from django import template
register = template.Library()


@register.inclusion_tag('blocks/tag_cloud.html')
def tag_cloud():
    return {'tags': BlogTag.structures.tag_cloud()}

@register.inclusion_tag('blocks/top_posts.html')
def top_posts():
    return {'posts': Post.objects.order_by('-views', '-create_time')[:5]}

@register.inclusion_tag('blocks/form.html')
def comment_form(post):
    return {'form': CommentForm(initial={'post': post})}