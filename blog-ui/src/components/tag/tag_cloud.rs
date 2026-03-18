use crate::api::types::{TagCloudItem, TagResult};
use crate::components::tag::TagBadge;
use leptos::prelude::*;

#[component]
pub fn TagCloud(items: Vec<TagCloudItem>) -> impl IntoView {
    view! {
        <div class="tag-cloud">
            {items
                .into_iter()
                .map(|tag| {
                    let tag_result = TagResult {
                        id: 0,
                        name: tag.name,
                        slug: tag.slug,
                    };
                    view! { <TagBadge tag=tag_result/> }
                })
                .collect_view()}
        </div>
    }
}
