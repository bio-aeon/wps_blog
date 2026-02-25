from datetime import timedelta

from django.contrib.admin.views.decorators import staff_member_required
from django.db.models import Count, Sum
from django.db.models.functions import TruncMonth
from django.shortcuts import render
from django.utils import timezone

from blog_admin.models import Post, Comment, Tag, Page, User


@staff_member_required
def dashboard_view(request):
    total_posts = Post.objects.count()
    published_posts = Post.objects.filter(is_hidden=False).count()
    draft_posts = Post.objects.filter(is_hidden=True).count()
    total_views = Post.objects.aggregate(total=Sum('views'))['total'] or 0

    total_comments = Comment.objects.count()
    pending_comments = Comment.objects.filter(is_approved=False).count()
    recent_comments = Comment.objects.select_related('post').order_by('-created_at')[:10]

    total_tags = Tag.objects.count()
    total_pages = Page.objects.count()
    total_users = User.objects.count()

    top_posts = Post.objects.filter(is_hidden=False).order_by('-views')[:5]
    recent_posts = Post.objects.order_by('-created_at')[:5]
    popular_tags = Tag.objects.annotate(
        post_count=Count('posttag')
    ).order_by('-post_count')[:10]

    context = {
        'title': 'Dashboard',
        'total_posts': total_posts,
        'published_posts': published_posts,
        'draft_posts': draft_posts,
        'total_views': total_views,
        'total_comments': total_comments,
        'pending_comments': pending_comments,
        'recent_comments': recent_comments,
        'total_tags': total_tags,
        'total_pages': total_pages,
        'total_users': total_users,
        'top_posts': top_posts,
        'recent_posts': recent_posts,
        'popular_tags': popular_tags,
    }
    return render(request, 'admin/dashboard.html', context)


@staff_member_required
def analytics_view(request):
    twelve_months_ago = timezone.now() - timedelta(days=365)

    posts_by_month = list(
        Post.objects
        .filter(created_at__gte=twelve_months_ago)
        .annotate(month=TruncMonth('created_at'))
        .values('month')
        .annotate(count=Count('id'))
        .order_by('month')
    )
    for entry in posts_by_month:
        entry['month'] = entry['month'].strftime('%Y-%m')

    top_posts = list(
        Post.objects
        .filter(is_hidden=False)
        .order_by('-views')[:10]
        .values('name', 'views')
    )

    comments_by_month = list(
        Comment.objects
        .filter(created_at__gte=twelve_months_ago)
        .annotate(month=TruncMonth('created_at'))
        .values('month')
        .annotate(count=Count('id'))
        .order_by('month')
    )
    for entry in comments_by_month:
        entry['month'] = entry['month'].strftime('%Y-%m')

    context = {
        'title': 'Analytics',
        'posts_by_month': posts_by_month,
        'top_posts': top_posts,
        'comments_by_month': comments_by_month,
    }
    return render(request, 'admin/analytics.html', context)
