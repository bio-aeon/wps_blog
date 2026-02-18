use crate::api::get_page;
use crate::components::common::{ErrorDisplay, StaticPageSkeleton};
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
                            <ErrorDisplay
                                title="Page not found".to_string()
                                message=e.to_string()
                                back_url="/".to_string()
                                back_label="← Back to Home".to_string()
                            />
                        }
                        .into_any()
                    }
                }
            })}
        </Suspense>
    }
}
