use leptos::prelude::*;

#[component]
pub fn Footer() -> impl IntoView {
    view! {
        <footer class="site-footer">
            <div class="container">
                <div class="footer-links">
                    <a href="/">"Home"</a>
                    <a href="/posts">"Blog"</a>
                    <a href="/tags">"Tags"</a>
                    <a href="/pages/about">"About"</a>
                </div>
                <div class="footer-social">
                    <a href="https://github.com" target="_blank" rel="noopener noreferrer">"GitHub"</a>
                </div>
                <p class="copyright">{"\u{00A9} 2026 WPS Blog"}</p>
            </div>
        </footer>
    }
}
