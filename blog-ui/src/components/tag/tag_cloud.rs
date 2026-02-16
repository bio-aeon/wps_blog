use crate::api::types::TagCloudItem;
use leptos::prelude::*;

const MIN_FONT_SIZE_REM: f64 = 0.8;
const MAX_FONT_SIZE_REM: f64 = 2.0;

#[component]
pub fn TagCloud(items: Vec<TagCloudItem>) -> impl IntoView {
    view! {
        <div class="tag-cloud">
            {items
                .into_iter()
                .map(|tag| {
                    let font_size = format!(
                        "{}rem",
                        MIN_FONT_SIZE_REM + tag.weight * (MAX_FONT_SIZE_REM - MIN_FONT_SIZE_REM),
                    );
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
