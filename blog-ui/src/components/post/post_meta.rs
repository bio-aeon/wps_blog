use crate::api::types::TagResult;
use crate::components::tag::TagBadge;
use super::format_date;
use leptos::prelude::*;

#[component]
pub fn PostMeta(
    created_at: String,
    tags: Vec<TagResult>,
    #[prop(optional)] reading_time: Option<usize>,
) -> impl IntoView {
    let date_display = format_date(&created_at);

    view! {
        <div class="post-meta">
            <time datetime=created_at>{date_display}</time>
            {reading_time.map(|mins| view! {
                <span class="meta-separator">"·"</span>
                <span class="reading-time">{format!("{} min read", mins)}</span>
            })}
            <div class="post-tags">
                {tags.into_iter().map(|tag| view! { <TagBadge tag=tag/> }).collect_view()}
            </div>
        </div>
    }
}
