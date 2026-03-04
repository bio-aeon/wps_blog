use crate::api::{get_post, increment_view};
use crate::components::comment::CommentThread;
use crate::components::common::{ErrorDisplay, PostDetailSkeleton};
use crate::components::post::toc::{extract_headings, inject_heading_ids, TableOfContents};
use crate::components::post::{estimate_reading_time, PostContent, PostMeta};
use leptos::prelude::*;
use leptos_meta::Title;
use leptos_router::hooks::use_params_map;

#[component]
pub fn PostDetailPage() -> impl IntoView {
    let params = use_params_map();
    let id = move || {
        params
            .read()
            .get("id")
            .and_then(|id| id.parse::<i32>().ok())
            .unwrap_or(0)
    };

    let post_resource = Resource::new(move || id(), |id| async move { get_post(id).await });

    // Fire-and-forget view increment
    Effect::new(move || {
        let post_id = id();
        if post_id > 0 {
            leptos::task::spawn_local(async move {
                let _ = increment_view(post_id).await;
            });
        }
    });

    view! {
        <Suspense fallback=move || {
            view! { <PostDetailSkeleton/> }
        }>
            {move || Suspend::new(async move {
                match post_resource.await {
                    Ok(post) => {
                        let reading_time = estimate_reading_time(&post.text);
                        let headings = extract_headings(&post.text);
                        let html_with_ids = inject_heading_ids(&post.text);

                        view! {
                            <Title text=format!("{} - WPS Blog", &post.name)/>
                            <article class="post-detail">
                                <h1 class="post-title">{post.name.clone()}</h1>
                                <PostMeta
                                    created_at=post.created_at.clone()
                                    tags=post.tags.clone()
                                    reading_time=reading_time
                                />
                                <TableOfContents entries=headings/>
                                <PostContent html_content=html_with_ids/>
                            </article>
                            <CommentThread post_id=id()/>
                            <a href="/posts" class="back-link">"← Back to posts"</a>
                        }
                        .into_any()
                    }
                    Err(e) => {
                        view! {
                            <ErrorDisplay
                                title="Post not found".to_string()
                                message=e.to_string()
                                back_url="/posts".to_string()
                                back_label="← Back to posts".to_string()
                            />
                        }
                        .into_any()
                    }
                }
            })}
        </Suspense>
    }
}
