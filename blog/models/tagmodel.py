# -*- coding: utf-8 -*-
from django.db import models
from django.db.models import Count

from taggit.models import Tag, TaggedItem
from wps_blog import utils


class TagStructuresManager(models.Manager):
    def tag_cloud(self):
        raw_tags = list(self.filter(post__hidden=False)
                            .values('tag__id', 'tag__name', 'tag__slug')
                            .annotate(posts_count=Count('post'))
                            .order_by('tag__name'))

        if len(raw_tags):
            min_count = raw_tags[0]['posts_count']
        else:
            min_count = 0
        max_count = 0

        for raw_tag in raw_tags:
            if raw_tag['posts_count'] < min_count:
                min_count = raw_tag['posts_count']

            if raw_tag['posts_count'] > max_count:
                max_count = raw_tag['posts_count']

        max_index = 5 # максимально возможный относительный вес тега
        tags = []
        if raw_tags:
            #на сколько различаются самое большое и самое малое количество постов тега
            dif = max_count - min_count if max_count != min_count else 1

            for raw_tag in raw_tags:
                index = int(round(float(raw_tag['posts_count'] - min_count) / dif * max_index))
                if not index:
                    index = 1

                tags.append({'name': raw_tag['tag__name'], 'slug': raw_tag['tag__slug'], 'index': index})
        return tags


class TaggedManager(models.Manager):
    def get_query_set(self):
        return super(TaggedManager, self).get_query_set().prefetch_related('tagged_items__tag')


class BlogTag(Tag):
    class Meta:
        proxy = True

    def slugify(self, tag, i=None):
        slug = utils.translit(tag.lower().replace(' ', '-'))
        if i is not None:
            slug += "_%d" % i
        return slug


class BlogTaggedItem(TaggedItem):
    structures = TagStructuresManager()
    objects = models.Manager()

    class Meta:
        proxy = True
        app_label = 'blog'

    @classmethod
    def tag_model(cls):
        return BlogTag