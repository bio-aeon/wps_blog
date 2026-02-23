from django.contrib import admin
from django.db.models import Count
from django.utils.html import format_html

from blog_admin.models import User, Post, Tag, PostTag, Comment, Page, Config
from blog_admin.forms import PostAdminForm, PageAdminForm


# -- Inlines ----------------------------------------------------------------

class PostTagInline(admin.TabularInline):
    model = PostTag
    extra = 1
    autocomplete_fields = ['tag']


class CommentInline(admin.TabularInline):
    model = Comment
    extra = 0
    readonly_fields = ('name', 'email', 'text', 'rating', 'is_approved', 'created_at')
    fields = ('name', 'email', 'text', 'rating', 'is_approved', 'created_at')
    can_delete = True
    show_change_link = True

    def has_add_permission(self, request, obj=None):
        return False


# -- User Admin --------------------------------------------------------------

@admin.register(User)
class UserAdmin(admin.ModelAdmin):
    list_display = ('username', 'email', 'is_active', 'is_admin', 'created_at')
    list_filter = ('is_active', 'is_admin')
    search_fields = ('username', 'email')
    readonly_fields = ('created_at',)
    ordering = ('-created_at',)

    fieldsets = (
        (None, {'fields': ('username', 'email', 'password')}),
        ('Permissions', {'fields': ('is_active', 'is_admin')}),
        ('Metadata', {'fields': ('created_at',)}),
    )

    def has_add_permission(self, request):
        return False


# -- Post Admin --------------------------------------------------------------

@admin.register(Post)
class PostAdmin(admin.ModelAdmin):
    form = PostAdminForm
    list_display = ('name', 'author', 'publication_status', 'views', 'tag_list', 'created_at')
    list_filter = ('is_hidden', 'author', 'created_at')
    search_fields = ('name', 'short_text', 'text')
    readonly_fields = ('views', 'created_at')
    date_hierarchy = 'created_at'
    ordering = ('-created_at',)
    inlines = [PostTagInline]
    actions = ['publish_posts', 'unpublish_posts']

    fieldsets = (
        (None, {
            'fields': ('name', 'short_text', 'text')
        }),
        ('Publishing', {
            'fields': ('author', 'is_hidden'),
            'description': 'Set "Is hidden" to False to publish the post.'
        }),
        ('SEO', {
            'fields': ('meta_title', 'meta_keywords', 'meta_description'),
            'classes': ('collapse',),
        }),
        ('Metadata', {
            'fields': ('views', 'created_at'),
        }),
    )

    def get_queryset(self, request):
        return super().get_queryset(request).prefetch_related('tags')

    @admin.display(description='Status', ordering='is_hidden')
    def publication_status(self, obj):
        if obj.is_hidden:
            return format_html('<span style="color: #e74c3c;">Draft</span>')
        return format_html('<span style="color: #27ae60;">Published</span>')

    @admin.display(description='Tags')
    def tag_list(self, obj):
        return ', '.join(tag.name for tag in obj.tags.all())

    @admin.action(description='Publish selected posts')
    def publish_posts(self, request, queryset):
        updated = queryset.update(is_hidden=False)
        self.message_user(request, f'{updated} post(s) published.')

    @admin.action(description='Unpublish selected posts (move to draft)')
    def unpublish_posts(self, request, queryset):
        updated = queryset.update(is_hidden=True)
        self.message_user(request, f'{updated} post(s) moved to draft.')


# -- Tag Admin ---------------------------------------------------------------

@admin.register(Tag)
class TagAdmin(admin.ModelAdmin):
    list_display = ('name', 'slug', 'post_count')
    search_fields = ('name', 'slug')
    prepopulated_fields = {'slug': ('name',)}
    ordering = ('name',)

    def get_queryset(self, request):
        return super().get_queryset(request).annotate(
            _post_count=Count('posttag')
        )

    @admin.display(description='Posts', ordering='_post_count')
    def post_count(self, obj):
        return obj._post_count


# -- Comment Admin ------------------------------------------------------------

@admin.register(Comment)
class CommentAdmin(admin.ModelAdmin):
    list_display = ('short_text_display', 'name', 'post', 'is_approved', 'rating', 'created_at')
    list_filter = ('is_approved', 'created_at')
    search_fields = ('name', 'email', 'text')
    readonly_fields = ('name', 'email', 'post', 'parent', 'rating', 'created_at')
    ordering = ('-created_at',)
    actions = ['approve_comments', 'reject_comments']
    list_per_page = 30

    fieldsets = (
        ('Content', {
            'fields': ('text', 'name', 'email')
        }),
        ('Context', {
            'fields': ('post', 'parent')
        }),
        ('Moderation', {
            'fields': ('is_approved', 'rating', 'created_at')
        }),
    )

    @admin.display(description='Comment')
    def short_text_display(self, obj):
        max_len = 80
        if len(obj.text) > max_len:
            return obj.text[:max_len] + '...'
        return obj.text

    @admin.action(description='Approve selected comments')
    def approve_comments(self, request, queryset):
        updated = queryset.update(is_approved=True)
        self.message_user(request, f'{updated} comment(s) approved.')

    @admin.action(description='Reject selected comments')
    def reject_comments(self, request, queryset):
        updated = queryset.update(is_approved=False)
        self.message_user(request, f'{updated} comment(s) rejected.')


# -- Page Admin ---------------------------------------------------------------

@admin.register(Page)
class PageAdmin(admin.ModelAdmin):
    form = PageAdminForm
    list_display = ('title', 'url', 'created_at')
    search_fields = ('title', 'url', 'content')
    readonly_fields = ('created_at',)
    ordering = ('title',)

    fieldsets = (
        (None, {
            'fields': ('title', 'url', 'content')
        }),
        ('Metadata', {
            'fields': ('created_at',)
        }),
    )


# -- Config Admin -------------------------------------------------------------

@admin.register(Config)
class ConfigAdmin(admin.ModelAdmin):
    list_display = ('name', 'value', 'comment', 'created_at')
    search_fields = ('name', 'value', 'comment')
    readonly_fields = ('created_at',)
    ordering = ('name',)
