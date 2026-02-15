use crate::api::get_posts;
use crate::components::common::Pagination;
use crate::components::post::PostCard;
use leptos::prelude::*;
use leptos_meta::Title;
use leptos_router::hooks::use_query_map;

const POSTS_PER_PAGE: i32 = 10;

#[component]
pub fn PostListPage() -> impl IntoView {
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
        move || page(),
        |page| async move {
            let offset = (page - 1) * POSTS_PER_PAGE;
            get_posts(POSTS_PER_PAGE, offset, None).await
        },
    );

    view! {
        <Title text="Blog Posts - WPS Blog"/>
        <h1>"Blog Posts"</h1>
        <Suspense fallback=move || {
            view! { <PostListSkeleton/> }
        }>
            {move || Suspend::new(async move {
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
                                base_url="/posts".to_string()
                            />
                        }
                        .into_any()
                    }
                    Err(e) => {
                        view! {
                            <div class="error-message">
                                <p>"Failed to load posts: " {e.to_string()}</p>
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
fn PostListSkeleton() -> impl IntoView {
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
