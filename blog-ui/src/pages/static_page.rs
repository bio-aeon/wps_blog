use crate::api::get_page;
use crate::components::common::{ErrorDisplay, StaticPageSkeleton};
use crate::i18n::{use_language, use_translations};
use leptos::prelude::*;
use leptos_meta::Title;
use leptos_router::hooks::use_params_map;

#[component]
pub fn StaticPageView() -> impl IntoView {
    let lang = use_language();
    let t = use_translations();
    let params = use_params_map();
    let url = move || params.read().get("url").unwrap_or_default();

    let page_resource = Resource::new(
        {
            let lang = lang.clone();
            move || (lang.clone(), url())
        },
        |(lang, url)| async move { get_page(lang, url).await },
    );

    view! {
        <Suspense fallback=move || {
            view! { <StaticPageSkeleton/> }
        }>
            {move || {
                let back_label = t.common.back_home;
                Suspend::new(async move {
                    match page_resource.await {
                        Ok(page) => {
                            view! {
                                <Title text=format!("{} - WPS Blog", &page.title)/>
                                <article class="static-page">
                                    <h1>{page.title.clone()}</h1>
                                    <div class="static-page-content" inner_html=page.content.clone().unwrap_or_default()></div>
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
                                    back_label=back_label.to_string()
                                />
                            }
                            .into_any()
                        }
                    }
                })
            }}
        </Suspense>
    }
}
