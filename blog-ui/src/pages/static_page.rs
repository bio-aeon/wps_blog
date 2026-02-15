use crate::api::get_page;
use leptos::prelude::*;
use leptos_meta::Title;
use leptos_router::hooks::use_params_map;

#[component]
pub fn StaticPageView() -> impl IntoView {
    let params = use_params_map();
    let url = move || params.read().get("url").unwrap_or_default();

    let page_resource = Resource::new(move || url(), |url| async move { get_page(url).await });

    view! {
        <Suspense fallback=move || {
            view! { <StaticPageSkeleton/> }
        }>
            {move || Suspend::new(async move {
                match page_resource.await {
                    Ok(page) => {
                        view! {
                            <Title text=format!("{} - WPS Blog", &page.title)/>
                            <article class="static-page">
                                <h1>{page.title.clone()}</h1>
                                <div class="static-page-content" inner_html=page.content.clone()></div>
                            </article>
                        }
                        .into_any()
                    }
                    Err(e) => {
                        view! {
                            <div class="error-message">
                                <h1>"Page not found"</h1>
                                <p>{e.to_string()}</p>
                                <a href="/">"← Back to Home"</a>
                            </div>
                        }
                        .into_any()
                    }
                }
            })}
        </Suspense>
    }
}

#[component]
fn StaticPageSkeleton() -> impl IntoView {
    view! {
        <div class="static-page-skeleton">
            <div class="skeleton-line title"></div>
            <div class="skeleton-line"></div>
            <div class="skeleton-line"></div>
            <div class="skeleton-line short"></div>
        </div>
    }
}
