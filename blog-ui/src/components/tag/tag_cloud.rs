use crate::api::types::TagCloudItem;
use leptos::prelude::*;

pub const MIN_FONT_SIZE_REM: f64 = 0.8;
pub const MAX_FONT_SIZE_REM: f64 = 2.0;

/// Maps a tag weight (0.0–1.0) to a font size in rem.
pub fn tag_font_size(weight: f64) -> f64 {
    MIN_FONT_SIZE_REM + weight * (MAX_FONT_SIZE_REM - MIN_FONT_SIZE_REM)
}

#[component]
pub fn TagCloud(items: Vec<TagCloudItem>) -> impl IntoView {
    view! {
        <div class="tag-cloud">
            {items
                .into_iter()
                .map(|tag| {
                    let font_size = format!("{}rem", tag_font_size(tag.weight));
                    let href = format!("/tags/{}", tag.slug);
                    view! {
                        <a href=href class="tag-cloud-item" style:font-size=font_size>
                            {tag.name}
                        </a>
                    }
                })
                .collect_view()}
        </div>
    }
}
