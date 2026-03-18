use crate::i18n::use_translations;
use leptos::prelude::*;

#[component]
pub fn Footer() -> impl IntoView {
    let t = use_translations();

    view! {
        <footer class="site-footer">
            <div class="footer-inner">
                <span class="copyright">{t.footer.copyright}</span>
                <nav class="footer-links" aria-label="Footer links">
                    <a href="/feed.xml">{t.footer.rss_feed}</a>
                    <a href="https://github.com" target="_blank" rel="noopener noreferrer">"GitHub"</a>
                    <a href="mailto:contact@example.com">"Email"</a>
                </nav>
            </div>
        </footer>
    }
}
