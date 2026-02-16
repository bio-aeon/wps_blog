use crate::api::search_posts;
use crate::components::common::Pagination;
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
            view! { <SearchSkeleton/> }
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
                                let base_url = format!("/search?q={}", urlencoding::encode(&current_q));
                                view! {
                                    <p class="search-result-count">
                                        {format!("{} result{} found", result.total, if result.total == 1 { "" } else { "s" })}
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
                                <div class="error-message">
                                    <p>"Search failed: " {e.to_string()}</p>
                                </div>
                            }
                            .into_any()
                        }
                    }
                })
            }}
        </Suspense>
    }
}

#[component]
fn SearchSkeleton() -> impl IntoView {
    view! {
        <div class="post-list-skeleton">
            {(0..3)
                .map(|_| {
                    view! {
                        <div class="post-card-skeleton">
                            <div class="skeleton-line title"></div>
                            <div class="skeleton-line meta"></div>
                            <div class="skeleton-line"></div>
                            <div class="skeleton-line short"></div>
                        </div>
                    }
                })
                .collect_view()}
        </div>
    }
}
