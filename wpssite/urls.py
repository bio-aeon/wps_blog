from django.conf.urls import patterns, include, url
from django.contrib import admin
from django.conf.urls.static import static
from django.conf import settings

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

urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
