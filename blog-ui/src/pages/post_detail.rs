use crate::api::{get_post, increment_view};
use crate::components::comment::CommentThread;
use crate::components::post::{PostContent, PostMeta};
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
                        view! {
                            <Title text=format!("{} - WPS Blog", &post.name)/>
                            <article class="post-detail">
                                <h1 class="post-title">{post.name.clone()}</h1>
                                <PostMeta created_at=post.created_at.clone() tags=post.tags.clone()/>
                                <PostContent html_content=post.text.clone()/>
                            </article>
                            <CommentThread post_id=id()/>
                            <a href="/posts" class="back-link">"← Back to posts"</a>
                        }
                        .into_any()
                    }
                    Err(e) => {
                        view! {
                            <div class="error-message">
                                <h1>"Post not found"</h1>
                                <p>{e.to_string()}</p>
                                <a href="/posts">"← Back to posts"</a>
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
fn PostDetailSkeleton() -> impl IntoView {
    view! {
        <div class="post-detail-skeleton">
            <div class="skeleton-line title"></div>
            <div class="skeleton-line meta"></div>
            <div class="skeleton-line"></div>
            <div class="skeleton-line"></div>
            <div class="skeleton-line short"></div>
            <div class="skeleton-line"></div>
            <div class="skeleton-line"></div>
            <div class="skeleton-line short"></div>
        </div>
    }
}
