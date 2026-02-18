use crate::api::get_posts;
use crate::components::common::{ErrorDisplay, Pagination, PostListSkeleton};
use crate::components::post::PostCard;
use leptos::prelude::*;
use leptos_meta::Title;
use leptos_router::hooks::{use_params_map, use_query_map};

const POSTS_PER_PAGE: i32 = 10;

#[component]
pub fn TagPostsPage() -> impl IntoView {
    let params = use_params_map();
    let query = use_query_map();

    let slug = move || params.read().get("slug").unwrap_or_default();
    let page = move || {
        query
            .read()
            .get("page")
            .and_then(|p| p.parse::<i32>().ok())
            .unwrap_or(1)
            .max(1)
    };

    let posts_resource = Resource::new(
        move || (slug(), page()),
        |(slug, page)| async move {
            let offset = (page - 1) * POSTS_PER_PAGE;
            get_posts(POSTS_PER_PAGE, offset, Some(slug)).await
        },
    );

    view! {
        <Suspense fallback=move || {
            view! { <PostListSkeleton/> }
        }>
            {move || {
                let current_slug = slug();
                let current_page = page();
                let base_url = format!("/tags/{}", current_slug);
                Suspend::new(async move {
                    match posts_resource.await {
                        Ok(result) => {
                            let title = format!("Posts tagged \"{}\" - WPS Blog", current_slug);
                            let heading = format!("Posts tagged \"{}\"", current_slug);
                            view! {
                                <Title text=title/>
                                <h1>{heading}</h1>
                                <a href="/tags" class="back-link">"← All tags"</a>
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
                                    back_url="/tags".to_string()
                                    back_label="← Back to tags".to_string()
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
