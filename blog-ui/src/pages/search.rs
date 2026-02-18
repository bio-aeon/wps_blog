use crate::api::search_posts;
use crate::components::common::{ErrorDisplay, Pagination, PostListSkeleton};
use crate::components::post::PostCard;
use crate::components::search::SearchBar;
use leptos::prelude::*;
use leptos_meta::Title;
use leptos_router::hooks::use_query_map;

const POSTS_PER_PAGE: i32 = 10;

#[component]
pub fn SearchPage() -> impl IntoView {
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
        move || (q(), page()),
        |(query, page)| async move {
            if query.is_empty() {
                return Ok(None);
            }
            let offset = (page - 1) * POSTS_PER_PAGE;
            search_posts(query, POSTS_PER_PAGE, offset).await.map(Some)
        },
    );

    let on_search = Callback::new(move |new_query: String| {
        let navigate = leptos_router::hooks::use_navigate();
        if new_query.is_empty() {
            navigate("/search", Default::default());
        } else {
            let encoded = urlencoding::encode(&new_query);
            navigate(&format!("/search?q={encoded}"), Default::default());
        }
    });

    view! {
        <Title text="Search - WPS Blog"/>
        <h1>"Search"</h1>
        <SearchBar initial_query=q() on_search=on_search/>
        <Suspense fallback=move || {
            view! { <PostListSkeleton/> }
        }>
            {move || {
                let current_q = q();
                let current_page = page();
                Suspend::new(async move {
                    match search_resource.await {
                        Ok(Some(result)) => {
                            if result.items.is_empty() {
                                let msg = format!("No posts found for \"{}\"", current_q);
                                view! {
                                    <p class="search-no-results">{msg}</p>
                                }
                                .into_any()
                            } else {
                                let base_url = format!(
                                    "/search?q={}",
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
                                <p class="search-prompt">"Enter a search term to find posts"</p>
                            }
                            .into_any()
                        }
                        Err(e) => {
                            view! {
                                <ErrorDisplay
                                    title="Search failed".to_string()
                                    message=e.to_string()
                                    back_url="/".to_string()
                                    back_label="← Back to Home".to_string()
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
