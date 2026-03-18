use crate::components::layout::{Footer, Header, Sidebar};
use crate::i18n::{is_supported_lang, provide_language_context, use_language_signal, DEFAULT_LANG};
use crate::pages::*;
use leptos::prelude::*;
use leptos_meta::{provide_meta_context, MetaTags, Stylesheet, Title};
use leptos_router::{
    components::{Outlet, ParentRoute, Redirect, Route, Router, Routes},
    hooks::use_params_map,
    path, StaticSegment,
};

pub fn shell(options: LeptosOptions) -> impl IntoView {
    view! {
        <!DOCTYPE html>
        <html lang="en">
            <head>
                <meta charset="utf-8"/>
                <meta name="viewport" content="width=device-width, initial-scale=1"/>
                <meta name="theme-color" content="#1a1b26"/>
                <link rel="icon" type="image/svg+xml" href="/assets/favicon.svg"/>
                <link rel="icon" type="image/x-icon" href="/assets/favicon.ico"/>
                <link rel="apple-touch-icon" sizes="180x180" href="/assets/apple-touch-icon.png"/>
                <link rel="manifest" href="/assets/site.webmanifest"/>
                <link rel="dns-prefetch" href="https://cdnjs.cloudflare.com"/>
                <link rel="preconnect" href="https://cdnjs.cloudflare.com" crossorigin="anonymous"/>
                {
                    leptos::html::link()
                        .attr("rel", "preload")
                        .attr("href", "/pkg/blog-ui.css")
                        .attr("as", "style")
                }
                <link rel="stylesheet" href="/assets/vendor/tokyo-night.css"/>
                <AutoReload options=options.clone()/>
                <HydrationScripts options=options.clone()/>
                <MetaTags/>
            </head>
            <body>
                <App/>
                <script>"if('serviceWorker' in navigator){navigator.serviceWorker.register('/assets/sw.js')}"</script>
                <script src="/assets/rum.js" defer></script>
            </body>
        </html>
    }
}

#[component]
pub fn App() -> impl IntoView {
    provide_meta_context();

    view! {
        <Stylesheet id="leptos" href="/pkg/blog-ui.css"/>
        <Title text="WPS Blog"/>
        <Router>
            <a class="skip-link" href="#main-content">"Skip to main content"</a>
            <Header/>
            <div class="page-layout container">
                <main class="content" id="main-content" tabindex="-1">
                    <Routes fallback=|| view! { <NotFoundPage/> }.into_any()>
                        <Route path=StaticSegment("") view=RootRedirect/>
                        <ParentRoute path=path!(":lang") view=LanguageWrapper>
                            <Route path=StaticSegment("") view=HomePage/>
                            <Route path=path!("posts") view=PostListPage/>
                            <Route path=path!("posts/:id") view=PostDetailPage/>
                            <Route path=path!("tags") view=TagListPage/>
                            <Route path=path!("tags/:slug") view=TagPostsPage/>
                            <Route path=path!("pages/:url") view=StaticPageView/>
                            <Route path=path!("about") view=AboutPage/>
                            <Route path=path!("contact") view=ContactPage/>
                            <Route path=path!("search") view=SearchPage/>
                        </ParentRoute>
                    </Routes>
                </main>
                <aside class="sidebar">
                    <Sidebar/>
                </aside>
            </div>
            <Footer/>
        </Router>
    }
}

#[component]
fn RootRedirect() -> impl IntoView {
    let default_path = format!("/{}/", DEFAULT_LANG);
    view! {
        <Redirect path=default_path/>
    }
}

#[component]
fn LanguageWrapper() -> impl IntoView {
    let params = use_params_map();
    let lang_from_url = move || {
        let raw = params.read().get("lang").unwrap_or_default();
        if is_supported_lang(&raw) {
            raw
        } else {
            DEFAULT_LANG.to_string()
        }
    };

    provide_language_context(lang_from_url());

    let lang_signal = use_language_signal();
    Effect::new(move || {
        let new_lang = lang_from_url();
        if lang_signal.get_untracked() != new_lang {
            lang_signal.set(new_lang);
        }
    });

    view! {
        <Outlet/>
    }
}
