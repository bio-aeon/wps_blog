use crate::components::layout::{Footer, Header, Sidebar};
use crate::pages::*;
use leptos::prelude::*;
use leptos_meta::{provide_meta_context, MetaTags, Stylesheet, Title};
use leptos_router::{
    components::{Route, Router, Routes},
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
                <link rel="stylesheet" href="/assets/vendor/tokyo-night.css"/>
                <AutoReload options=options.clone()/>
                <HydrationScripts options=options.clone()/>
                <MetaTags/>
            </head>
            <body>
                <App/>
                <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/prism.min.js" data-manual defer></script>
                <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-rust.min.js" defer></script>
                <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-java.min.js" defer></script>
                <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-scala.min.js" defer></script>
                <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-python.min.js" defer></script>
                <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-sql.min.js" defer></script>
                <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-bash.min.js" defer></script>
                <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-typescript.min.js" defer></script>
                <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-json.min.js" defer></script>
                <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-yaml.min.js" defer></script>
                <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-toml.min.js" defer></script>
                <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-haskell.min.js" defer></script>
                <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-idris.min.js" defer></script>
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
                        <Route path=StaticSegment("") view=HomePage/>
                        <Route path=path!("/posts") view=PostListPage/>
                        <Route path=path!("/posts/:id") view=PostDetailPage/>
                        <Route path=path!("/tags") view=TagListPage/>
                        <Route path=path!("/tags/:slug") view=TagPostsPage/>
                        <Route path=path!("/pages/:url") view=StaticPageView/>
                        <Route path=path!("/about") view=AboutPage/>
                        <Route path=path!("/contact") view=ContactPage/>
                        <Route path=path!("/search") view=SearchPage/>
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
