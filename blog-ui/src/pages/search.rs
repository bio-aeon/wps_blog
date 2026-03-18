use crate::api::search_posts;
use crate::components::common::{ErrorDisplay, Pagination, PostListSkeleton};
use crate::components::post::PostCard;
use crate::components::search::SearchBar;
use crate::i18n::{lang_href, use_language, use_translations};
use leptos::prelude::*;
use leptos_meta::Title;
use leptos_router::hooks::use_query_map;

const POSTS_PER_PAGE: i32 = 10;

#[component]
pub fn SearchPage() -> impl IntoView {
    let lang = use_language();
    let t = use_translations();
    let query_map = use_query_map();

    let q = move || query_map.read().get("q").unwrap_or_default();
    let page = move || {
        query_map
            .read()
            .get("page")
            .and_then(|p| p.parse::<i32>().ok())
            .unwrap_or(1)
            .max(1)
    };

    let search_resource = Resource::new(
        {
            let lang = lang.clone();
            move || (lang.clone(), q(), page())
        },
        |(lang, query, page)| async move {
            if query.is_empty() {
                return Ok(None);
            }
            let offset = (page - 1) * POSTS_PER_PAGE;
            search_posts(lang, query, POSTS_PER_PAGE, offset).await.map(Some)
        },
    );

    let search_href = lang_href(&lang, "/search");

    let on_search = {
        let search_href = search_href.clone();
        Callback::new(move |new_query: String| {
            let navigate = leptos_router::hooks::use_navigate();
            if new_query.is_empty() {
                navigate(&search_href, Default::default());
            } else {
                let encoded = urlencoding::encode(&new_query);
                navigate(&format!("{}?q={encoded}", search_href), Default::default());
            }
        })
    };

    view! {
        <Title text=format!("{} - WPS Blog", t.search.title)/>
        <h1>{t.search.title}</h1>
        <SearchBar initial_query=q() on_search=on_search/>
        <Suspense fallback=move || {
            view! { <PostListSkeleton/> }
        }>
            {move || {
                let current_q = q();
                let current_page = page();
                let search_href = search_href.clone();
                let back_label = t.common.back_home;
                let no_results_text = t.search.no_results;
                let search_prompt = t.search.placeholder;
                Suspend::new(async move {
                    match search_resource.await {
                        Ok(Some(result)) => {
                            if result.items.is_empty() {
                                let msg = format!("{} \"{}\"", no_results_text, current_q);
                                view! {
                                    <p class="search-no-results">{msg}</p>
                                }
                                .into_any()
                            } else {
                                let base_url = format!(
                                    "{}?q={}",
                                    search_href,
                                    urlencoding::encode(&current_q),
                                );
                                view! {
                                    <p class="search-result-count">
                                        {format!(
                                            "{} result{} found",
                                            result.total,
                                            if result.total == 1 { "" } else { "s" },
                                        )}
                                    </p>
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
                        }
                        Ok(None) => {
                            view! {
                                <p class="search-prompt">{search_prompt}</p>
                            }
                            .into_any()
                        }
                        Err(e) => {
                            view! {
                                <ErrorDisplay
                                    title="Search failed".to_string()
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
