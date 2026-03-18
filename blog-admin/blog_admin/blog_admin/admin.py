from django.contrib import admin
from django.db.models import Count
from django.utils.html import format_html, mark_safe

from blog_admin.models import (
    User, Post, Tag, PostTag, Comment, Page, Config,
    Skill, Experience, SocialLink, ContactSubmission,
    Language, PostTranslation, PageTranslation, TagTranslation,
)
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


class PostTranslationInline(admin.StackedInline):
    model = PostTranslation
    extra = 0
    fields = [
        'language', 'name', 'short_text', 'text',
        'seo_title', 'seo_description', 'seo_keywords', 'translation_status',
    ]

    def formfield_for_foreignkey(self, db_field, request, **kwargs):
        if db_field.name == 'language':
            kwargs['queryset'] = Language.objects.filter(is_active=True).order_by('sort_order')
        return super().formfield_for_foreignkey(db_field, request, **kwargs)


class PageTranslationInline(admin.StackedInline):
    model = PageTranslation
    extra = 0
    fields = [
        'language', 'title', 'content',
        'seo_title', 'seo_description', 'translation_status',
    ]

    def formfield_for_foreignkey(self, db_field, request, **kwargs):
        if db_field.name == 'language':
            kwargs['queryset'] = Language.objects.filter(is_active=True).order_by('sort_order')
        return super().formfield_for_foreignkey(db_field, request, **kwargs)


class TagTranslationInline(admin.TabularInline):
    model = TagTranslation
    extra = 0
    fields = ['language', 'name']

    def formfield_for_foreignkey(self, db_field, request, **kwargs):
        if db_field.name == 'language':
            kwargs['queryset'] = Language.objects.filter(is_active=True).order_by('sort_order')
        return super().formfield_for_foreignkey(db_field, request, **kwargs)


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
    list_display = (
        'name', 'author', 'publication_status', 'views', 'tag_list',
        'translation_coverage', 'created_at',
    )
    list_filter = ('is_hidden', 'author', 'created_at')
    search_fields = ('name', 'short_text', 'text')
    readonly_fields = ('views', 'created_at')
    date_hierarchy = 'created_at'
    ordering = ('-created_at',)
    inlines = [PostTagInline, CommentInline, PostTranslationInline]
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
            return mark_safe('<span style="color: #e74c3c;">Draft</span>')
        return mark_safe('<span style="color: #27ae60;">Published</span>')

    @admin.display(description='Tags')
    def tag_list(self, obj):
        return ', '.join(tag.name for tag in obj.tags.all())

    def translation_coverage(self, obj):
        translations = obj.translations.filter(translation_status='published')
        langs = sorted([t.language_id for t in translations])
        return ', '.join(langs) if langs else '—'
    translation_coverage.short_description = 'Translations'

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
    inlines = [TagTranslationInline]

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
    inlines = [PageTranslationInline]

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


# -- Language Admin ------------------------------------------------------------

@admin.register(Language)
class LanguageAdmin(admin.ModelAdmin):
    list_display = ['code', 'name', 'native_name', 'is_default', 'is_active', 'sort_order']
    list_editable = ['is_active', 'sort_order']
    ordering = ['sort_order']


# -- Skill Admin --------------------------------------------------------------

@admin.register(Skill)
class SkillAdmin(admin.ModelAdmin):
    list_display = ('name', 'category', 'proficiency', 'sort_order', 'is_active')
    list_filter = ('category', 'is_active')
    list_editable = ('sort_order', 'is_active', 'proficiency')
    search_fields = ('name', 'category')
    prepopulated_fields = {'slug': ('name',)}
    ordering = ('sort_order', 'name')


# -- Experience Admin ----------------------------------------------------------

@admin.register(Experience)
class ExperienceAdmin(admin.ModelAdmin):
    list_display = ('position', 'company', 'start_date', 'end_date', 'is_current', 'sort_order', 'is_active')
    list_filter = ('is_active',)
    list_editable = ('sort_order', 'is_active')
    search_fields = ('company', 'position')
    ordering = ('sort_order', '-start_date')
    fieldsets = (
        (None, {'fields': ('company', 'position', 'description')}),
        ('Dates & Location', {'fields': ('start_date', 'end_date', 'location', 'company_url')}),
        ('Display', {'fields': ('sort_order', 'is_active')}),
    )

    @admin.display(boolean=True, description='Current')
    def is_current(self, obj):
        return obj.end_date is None


# -- Social Link Admin ---------------------------------------------------------

@admin.register(SocialLink)
class SocialLinkAdmin(admin.ModelAdmin):
    list_display = ('platform', 'label', 'url', 'sort_order', 'is_active')
    list_filter = ('platform', 'is_active')
    list_editable = ('sort_order', 'is_active')
    ordering = ('sort_order',)


# -- Contact Submission Admin --------------------------------------------------

@admin.register(ContactSubmission)
class ContactSubmissionAdmin(admin.ModelAdmin):
    list_display = ('subject', 'name', 'email', 'is_read', 'created_at')
    list_filter = ('is_read',)
    search_fields = ('name', 'email', 'subject', 'message')
    readonly_fields = ('name', 'email', 'subject', 'message', 'ip_address', 'created_at')
    list_per_page = 30
    actions = ['mark_as_read', 'mark_as_unread']
    date_hierarchy = 'created_at'

    def has_add_permission(self, request):
        return False

    @admin.action(description='Mark selected submissions as read')
    def mark_as_read(self, request, queryset):
        queryset.update(is_read=True)

    @admin.action(description='Mark selected submissions as unread')
    def mark_as_unread(self, request, queryset):
        queryset.update(is_read=False)

