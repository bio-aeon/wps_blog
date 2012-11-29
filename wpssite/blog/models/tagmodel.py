# -*- coding: utf-8 -*-
from django.db import models
from taggit.models import Tag, TaggedItem
from django.db.models import Count, Min, Max
from wpssite.utils import Helper


class TagStructuresManager(models.Manager):
    def tag_cloud(self):
        raw_tags = self.annotate(posts_count=Count('taggit_taggeditem_items')).order_by('name')
        stat = raw_tags.aggregate(posts_min=Min('posts_count'), posts_max=Max('posts_count'))
        min_count, max_count = int(stat['posts_min'] or 0), int(stat['posts_max'] or 0)
        max_index = 5 # максимально возможный относительный вес тега

        tags = []
        if(raw_tags):
            #на сколько различаются самое большое и самое малое количество постов тега
            dif = max_count - min_count if max_count != min_count else 1

            for raw_tag in raw_tags:
                index = int(round(float(raw_tag.posts_count - min_count) / dif * max_index))
                if not index:
                    index = 1

                tags.append({'name': raw_tag.name, 'slug': raw_tag.slug, 'index': index})
        return tags


class TaggedManager(models.Manager):
    def get_query_set(self):
        return super(TaggedManager, self).get_query_set().prefetch_related('tagged_items__tag')


class BlogTag(Tag):
    structures = TagStructuresManager()
    objects = models.Manager()

    class Meta:
        proxy = True

    def slugify(self, tag, i=None):
        slug = Helper.translit(tag.lower().replace(' ', '-'))
        if i is not None:
            slug += "_%d" % i
        return slug


class BlogTaggedItem(TaggedItem):
    class Meta:
        proxy = True
        app_label = 'blog'

    @classmethod
    def tag_model(cls):
        return BlogTag