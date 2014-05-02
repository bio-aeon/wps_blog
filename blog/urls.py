from django.conf.urls import patterns, include, url

urlpatterns = patterns('blog.views',
    url(r'^$', 'Posts', name='posts-index'),
    url(r'^tag/(\w*)/$', 'Posts', kwargs={'action': 'tag'}, name='posts-tag'),
    url(r'^(\d+)/$', 'Posts', kwargs={'action': 'view'}, name='posts-view'),
    url(r'^(?P<action>\w+)/$', 'Posts', name='posts-action'),

    url(r'^comment/create/$', 'Comments', kwargs={'action': 'create'}, name='comments-create'),
    url(r'^comment/rate/(\d+)/(up|down)/$', 'Comments', kwargs={'action': 'rate'}, name='comments-rate'),
)