use crate::api::types::ListPostResult;
use crate::components::tag::TagBadge;
use super::format_date;
use leptos::prelude::*;

#[component]
pub fn PostCard(post: ListPostResult) -> impl IntoView {
    let href = format!("/posts/{}", post.id);
    let date_display = format_date(&post.created_at);

    view! {
        <article class="post-card">
            <h2><a href=href.clone()>{post.name.clone()}</a></h2>
            <div class="post-meta">
                <time datetime=post.created_at.clone()>{date_display}</time>
                <div class="post-tags">
                    {post.tags.into_iter().map(|tag| view! { <TagBadge tag=tag/> }).collect_view()}
                </div>
            </div>
            <p class="post-excerpt">{post.short_text.clone()}</p>
            <a href=href class="read-more">"Read more →"</a>
        </article>
    }
}
