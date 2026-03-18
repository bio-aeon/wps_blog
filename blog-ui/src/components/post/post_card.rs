use crate::api::types::ListPostResult;
use crate::components::tag::TagBadge;
use crate::i18n::{lang_href, use_language, use_translations};
use super::{estimate_reading_time, format_date};
use leptos::prelude::*;

#[component]
pub fn PostCard(post: ListPostResult) -> impl IntoView {
    let lang = use_language();
    let t = use_translations();
    let href = lang_href(&lang, &format!("/posts/{}", post.id));
    let date_display = format_date(&post.created_at);
    let reading_time = estimate_reading_time(post.short_text.as_deref().unwrap_or(""));

    view! {
        <article class="post-card">
            <h2><a href=href.clone()>{post.name.clone()}</a></h2>
            <div class="post-meta">
                <time datetime=post.created_at.clone()>{date_display}</time>
                <span class="meta-separator">"·"</span>
                <span class="reading-time">{format!("{} {}", reading_time, t.post.reading_time)}</span>
            </div>
            <p class="post-excerpt">{post.short_text.clone().unwrap_or_default()}</p>
            <div class="post-tags">
                {post.tags.into_iter().map(|tag| view! { <TagBadge tag=tag/> }).collect_view()}
            </div>
        </article>
    }
}
