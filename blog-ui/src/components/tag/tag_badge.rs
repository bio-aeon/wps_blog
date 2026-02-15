use crate::api::types::TagResult;
use leptos::prelude::*;

#[component]
pub fn TagBadge(tag: TagResult) -> impl IntoView {
    let href = format!("/tags/{}", tag.slug);

    view! {
        <a href=href class="tag-badge">{tag.name}</a>
    }
}
