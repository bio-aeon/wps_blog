from django.urls import path

from blog_admin.assistant import views

app_name = 'assistant'

urlpatterns = [
    # Dashboard & Composers (HTML views)
    path('', views.assistant_dashboard, name='dashboard'),
    path('compose/blog/', views.blog_composer, name='blog-composer'),
    path('compose/linkedin/', views.linkedin_composer, name='linkedin-composer'),
    path('compose/twitter/', views.twitter_composer, name='twitter-composer'),

    # Suggestions (JSON API)
    path('suggest/<str:platform>/', views.suggest_topics, name='suggest'),

    # Generation (JSON API)
    path('generate/<str:platform>/', views.generate_content, name='generate'),
    path('refine/', views.refine_content, name='refine'),
    path('adjust-tone/', views.adjust_tone, name='adjust-tone'),
    path('seo-suggest/', views.seo_suggest, name='seo-suggest'),
    path('repurpose/', views.repurpose_content, name='repurpose'),

    # Drafts (JSON API)
    path('drafts/', views.draft_list_create, name='drafts'),
    path('drafts/<int:pk>/', views.draft_detail, name='draft-detail'),
    path('drafts/<int:pk>/publish/', views.draft_publish, name='draft-publish'),

    # Templates (JSON API)
    path('templates/', views.template_list_create, name='templates'),
    path('templates/<int:pk>/', views.template_detail, name='template-detail'),

    # History (JSON API, read-only)
    path('history/', views.generation_history_list, name='history'),
]
