# -*- coding: utf-8 -*-
from django import template
from ..models.tagmodel import BlogTaggedItem
from ..models.postmodel import Post
from ..forms import CommentForm

register = template.Library()


@register.inclusion_tag('blocks/tag_cloud.html')
def tag_cloud():
    return {'tags': BlogTaggedItem.structures.tag_cloud()}

@register.inclusion_tag('blocks/top_posts.html')
def top_posts():
    return {'posts': Post.objects.order_by('-views', '-create_time')[:5]}

@register.inclusion_tag('blocks/form.html')
def comment_form(post):
    return {'form': CommentForm(initial={'post': post})}