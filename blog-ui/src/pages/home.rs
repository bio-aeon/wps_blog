use crate::api::get_recent_posts;
use crate::components::common::{ErrorDisplay, PostListSkeleton};
use crate::components::post::PostCard;
use leptos::prelude::*;
use leptos_meta::Title;

const RECENT_POSTS_COUNT: i32 = 5;

#[component]
pub fn HomePage() -> impl IntoView {
    let recent_posts = Resource::new(|| (), |_| get_recent_posts(RECENT_POSTS_COUNT));

    view! {
        <Title text="WPS Blog"/>
        <section class="home-hero">
            <h1>"WPS Blog"</h1>
            <p class="home-subtitle">
                "A personal blog about software engineering, technology, and more."
            </p>
        </section>
        <section class="home-recent">
            <h2>"Recent Posts"</h2>
            <Suspense fallback=move || {
                view! { <PostListSkeleton count=RECENT_POSTS_COUNT as usize/> }
            }>
                {move || Suspend::new(async move {
                    match recent_posts.await {
                        Ok(posts) => {
                            view! {
                                <div class="post-list">
                                    {posts
                                        .into_iter()
                                        .map(|post| view! { <PostCard post=post/> })
                                        .collect_view()}
                                </div>
                                <a href="/posts" class="view-all-link">"View all posts →"</a>
                            }
                            .into_any()
                        }
                        Err(e) => {
                            view! {
                                <ErrorDisplay
                                    title="Failed to load posts".to_string()
                                    message=e.to_string()
                                />
                            }
                            .into_any()
                        }
                    }
                })}
            </Suspense>
        </section>
    }
}
