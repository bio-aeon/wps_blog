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
                <AutoReload options=options.clone()/>
                <HydrationScripts options=options.clone()/>
                <MetaTags/>
            </head>
            <body>
                <App/>
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
            <Header/>
            <div class="page-layout container">
                <main class="content">
                    <Routes fallback=|| view! { <NotFoundPage/> }.into_any()>
                        <Route path=StaticSegment("") view=HomePage/>
                        <Route path=path!("/posts") view=PostListPage/>
                        <Route path=path!("/posts/:id") view=PostDetailPage/>
                        <Route path=path!("/tags") view=TagListPage/>
                        <Route path=path!("/tags/:slug") view=TagPostsPage/>
                        <Route path=path!("/pages/:url") view=StaticPageView/>
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

#[component]
fn HomePage() -> impl IntoView {
    view! {
        <h1>"WPS Blog"</h1>
        <p>"Welcome to WPS Blog. Site is under construction."</p>
    }
}
