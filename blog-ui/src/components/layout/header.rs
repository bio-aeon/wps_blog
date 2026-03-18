use crate::components::layout::LanguageSwitcher;
use crate::i18n::{lang_href, use_language, use_translations};
use leptos::prelude::*;
use leptos_router::hooks::use_location;

#[component]
pub fn Header() -> impl IntoView {
    let location = use_location();
    let pathname = move || location.pathname.get();
    let mobile_menu_open = RwSignal::new(false);

    let toggle_menu = move |_| {
        *mobile_menu_open.write() = !mobile_menu_open.get();
    };

    let close_menu = move |_| {
        mobile_menu_open.set(false);
    };

    let t = use_translations();
    let lang = use_language();

    let home_href = lang_href(&lang, "/");
    let posts_href = lang_href(&lang, "/posts");
    let tags_href = lang_href(&lang, "/tags");
    let about_href = lang_href(&lang, "/about");
    let contact_href = lang_href(&lang, "/contact");
    let lang_prefix = format!("/{}", lang);

    view! {
        <header class="site-header">
            <div class="header-inner">
                <a class="logo" href=home_href.clone()>"WPS Blog"</a>
                <nav
                    class="header-nav"
                    id="main-nav"
                    aria-label="Main navigation"
                    class:open=move || mobile_menu_open.get()
                    on:keydown=move |ev: leptos::ev::KeyboardEvent| {
                        if ev.key() == "Escape" {
                            mobile_menu_open.set(false);
                        }
                    }
                >
                    <a
                        href=home_href
                        class:active={
                            let lang_prefix = lang_prefix.clone();
                            move || {
                                let p = pathname();
                                p == format!("{}/", lang_prefix) || p == lang_prefix.clone()
                            }
                        }
                        on:click=close_menu
                    >
                        {t.nav.home}
                    </a>
                    <a
                        href=posts_href
                        class:active={
                            let lang_prefix = lang_prefix.clone();
                            move || pathname().starts_with(&format!("{}/posts", lang_prefix))
                        }
                        on:click=close_menu
                    >
                        {t.nav.posts}
                    </a>
                    <a
                        href=tags_href
                        class:active={
                            let lang_prefix = lang_prefix.clone();
                            move || pathname().starts_with(&format!("{}/tags", lang_prefix))
                        }
                        on:click=close_menu
                    >
                        {t.nav.tags}
                    </a>
                    <a
                        href=about_href
                        class:active={
                            let lang_prefix = lang_prefix.clone();
                            move || pathname() == format!("{}/about", lang_prefix)
                        }
                        on:click=close_menu
                    >
                        {t.nav.about}
                    </a>
                    <a
                        href=contact_href
                        class:active={
                            let lang_prefix = lang_prefix.clone();
                            move || pathname() == format!("{}/contact", lang_prefix)
                        }
                        on:click=close_menu
                    >
                        {t.nav.contact}
                    </a>
                    <div class="header-search" role="search">
                        <input
                            type="search"
                            placeholder=t.nav.search_placeholder
                            aria-label=t.nav.search_placeholder
                        />
                    </div>
                    <LanguageSwitcher/>
                </nav>
                <button
                    class="mobile-menu-toggle"
                    aria-label="Toggle menu"
                    aria-expanded=move || mobile_menu_open.get().to_string()
                    aria-controls="main-nav"
                    on:click=toggle_menu
                >
                    <span class="hamburger" aria-hidden="true"></span>
                </button>
            </div>
        </header>
    }
}
