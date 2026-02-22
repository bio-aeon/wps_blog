use leptos::prelude::*;

#[component]
pub fn Footer() -> impl IntoView {
    view! {
        <footer class="site-footer">
            <div class="footer-inner">
                <span class="copyright">{"\u{00A9} 2026 WPS Blog"}</span>
                <div class="footer-links">
                    <a href="/feed.xml">"RSS"</a>
                    <a href="https://github.com" target="_blank" rel="noopener noreferrer">"GitHub"</a>
                    <a href="mailto:contact@example.com">"Email"</a>
                </div>
            </div>
        </footer>
    }
}
