from django.contrib.admin.views.decorators import staff_member_required
from django.db.models import Count, Sum
from django.shortcuts import render

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
