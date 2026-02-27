use crate::api::types::SocialLinkResult;
use leptos::prelude::*;

#[component]
pub fn SocialLinks(links: Vec<SocialLinkResult>) -> impl IntoView {
    view! {
        <div class="social-links">
            {links
                .into_iter()
                .map(|link| {
                    let display = link.label.clone().unwrap_or_else(|| link.platform.clone());
                    let class_name = format!("social-link social-link-{}", link.platform);
                    view! {
                        <a href=link.url class=class_name target="_blank" rel="noopener">
                            {display}
                        </a>
                    }
                })
                .collect_view()}
        </div>
    }
}
