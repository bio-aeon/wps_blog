use crate::api::get_posts;
use crate::components::common::{ErrorDisplay, Pagination, PostListSkeleton};
use crate::components::post::PostCard;
use crate::i18n::{lang_href, use_language, use_translations};
use leptos::prelude::*;
use leptos_meta::Title;
use leptos_router::hooks::use_query_map;

const POSTS_PER_PAGE: i32 = 10;

#[component]
pub fn PostListPage() -> impl IntoView {
    let lang = use_language();
    let t = use_translations();
    let query = use_query_map();
    let page = move || {
        query
            .read()
            .get("page")
            .and_then(|p| p.parse::<i32>().ok())
            .unwrap_or(1)
            .max(1)
    };

    let posts_resource = Resource::new(
        {
            let lang = lang.clone();
            move || (lang.clone(), page())
        },
        |(lang, page)| async move {
            let offset = (page - 1) * POSTS_PER_PAGE;
            get_posts(lang, POSTS_PER_PAGE, offset, None).await
        },
    );

    let base_url = lang_href(&lang, "/posts");

    view! {
        <Title text=format!("{} - WPS Blog", t.nav.posts)/>
        <h1>{t.nav.posts}</h1>
        <Suspense fallback=move || {
            view! { <PostListSkeleton/> }
        }>
            {move || {
                let base_url = base_url.clone();
                let back_label = t.common.back_home;
                Suspend::new(async move {
                    match posts_resource.await {
                        Ok(result) => {
                            let current_page = page();
                            view! {
                                <div class="post-list">
                                    {result
                                        .items
                                        .into_iter()
                                        .map(|post| view! { <PostCard post=post/> })
                                        .collect_view()}
                                </div>
                                <Pagination
                                    current_page=current_page
                                    total_items=result.total
                                    items_per_page=POSTS_PER_PAGE
                                    base_url=base_url
                                />
                            }
                            .into_any()
                        }
                        Err(e) => {
                            view! {
                                <ErrorDisplay
                                    title="Failed to load posts".to_string()
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
