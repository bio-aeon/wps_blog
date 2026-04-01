from django.shortcuts import get_object_or_404

from blog_admin.models import (
    ContentDraft, Post, PostTranslation, Language,
)


VALID_PLATFORMS = {'blog', 'linkedin', 'twitter'}
VALID_STATUSES = {
    'idea', 'generating', 'draft', 'review',
    'approved', 'published', 'archived',
}


def list_drafts(platform=None, status=None, limit=20, offset=0):
    qs = ContentDraft.objects.all()
    if platform:
        qs = qs.filter(platform=platform)
    if status:
        qs = qs.filter(status=status)
    total = qs.count()
    drafts = list(qs[offset:offset + limit])
    return {'drafts': drafts, 'total': total}


def get_draft(draft_id):
    return get_object_or_404(ContentDraft, pk=draft_id)


def create_draft(platform, body, title=None, language_code='en',
                 status='draft', metadata=None, source_post_id=None):
    language = get_object_or_404(Language, code=language_code)
    kwargs = {
        'platform': platform,
        'body': body,
        'title': title,
        'language': language,
        'status': status,
        'metadata': metadata or {},
    }
    if source_post_id:
        kwargs['source_post'] = get_object_or_404(Post, pk=source_post_id)
    return ContentDraft.objects.create(**kwargs)


def update_draft(draft_id, **fields):
    draft = get_object_or_404(ContentDraft, pk=draft_id)
    allowed = {'title', 'body', 'status', 'metadata', 'language_code'}
    for key, value in fields.items():
        if key not in allowed:
            continue
        if key == 'language_code':
            draft.language = get_object_or_404(Language, code=value)
        else:
            setattr(draft, key, value)
    draft.save()
    return draft


def delete_draft(draft_id):
    draft = get_object_or_404(ContentDraft, pk=draft_id)
    draft.delete()


def publish_to_blog(draft_id, author):
    """Convert an approved blog draft into a published Post."""
    draft = get_object_or_404(ContentDraft, pk=draft_id)

    if draft.platform != 'blog':
        raise ValueError("Only blog drafts can be published to the blog.")
    if draft.status != 'approved':
        raise ValueError("Draft must have 'approved' status to publish.")

    short_text = (draft.body[:200] + '...') if len(draft.body) > 200 else draft.body

    post = Post.objects.create(
        name=draft.title or '(untitled)',
        short_text=short_text,
        text=draft.body,
        author=author,
        meta_title=draft.title or '',
        meta_keywords='',
        meta_description='',
        is_hidden=False,
    )

    PostTranslation.objects.create(
        post=post,
        language=draft.language,
        name=draft.title or '(untitled)',
        text=draft.body,
        short_text=short_text,
        translation_status='published',
    )

    draft.status = 'published'
    metadata = draft.metadata or {}
    metadata['published_post_id'] = post.pk
    draft.metadata = metadata
    draft.save()

    return post


def draft_to_dict(draft):
    return {
        'id': draft.pk,
        'platform': draft.platform,
        'title': draft.title,
        'body': draft.body,
        'status': draft.status,
        'language_code': draft.language_id,
        'source_post_id': draft.source_post_id,
        'metadata': draft.metadata,
        'created_at': draft.created_at.isoformat() if draft.created_at else None,
        'updated_at': draft.updated_at.isoformat() if draft.updated_at else None,
    }
