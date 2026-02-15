use leptos::prelude::*;

#[component]
pub fn PostContent(html_content: String) -> impl IntoView {
    view! {
        <div class="post-content" inner_html=html_content></div>
    }
}
