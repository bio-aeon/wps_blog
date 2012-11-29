from django.conf.urls import patterns, include, url
from django.contrib import admin

admin.autodiscover()

urlpatterns = patterns('wpssite.blog.views',
    url(r'^$', 'Posts', {'action': 'start'}, name='posts-start'),
    url(r'^search/$', 'Posts', {'action': 'search'}, name='posts-search'),
)

urlpatterns += patterns('',
    url(r'^blog/', include('wpssite.blog.urls')),
    url(r'^page/', include('django.contrib.flatpages.urls')),
    url(r'^djadmin/', include(admin.site.urls)),
)
