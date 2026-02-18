use crate::api::{get_recent_posts, get_tag_cloud};
use crate::components::common::SidebarSkeleton;
use crate::components::tag::TagCloud;
use leptos::prelude::*;

const RECENT_POSTS_COUNT: i32 = 5;

#[component]
pub fn Sidebar() -> impl IntoView {
    let recent_posts = Resource::new(|| (), |_| get_recent_posts(RECENT_POSTS_COUNT));
    let tag_cloud = Resource::new(|| (), |_| get_tag_cloud());

    view! {
        <div class="sidebar-section">
            <h3 class="sidebar-heading">"Recent Posts"</h3>
            <Suspense fallback=move || view! { <SidebarSkeleton/> }>
                {move || Suspend::new(async move {
                    match recent_posts.await {
                        Ok(posts) => {
                            view! {
                                <ul class="recent-posts-list">
                                    {posts
                                        .into_iter()
                                        .map(|post| {
                                            let href = format!("/posts/{}", post.id);
                                            view! {
                                                <li>
                                                    <a href=href>{post.name}</a>
                                                </li>
                                            }
                                        })
                                        .collect_view()}
                                </ul>
                            }
                            .into_any()
                        }
                        Err(_) => view! { <p class="sidebar-error">"Failed to load posts."</p> }.into_any(),
                    }
                })}
            </Suspense>
        </div>
        <div class="sidebar-section">
            <h3 class="sidebar-heading">"Tags"</h3>
            <Suspense fallback=move || view! { <SidebarSkeleton/> }>
                {move || Suspend::new(async move {
                    match tag_cloud.await {
                        Ok(cloud) => {
                            view! { <TagCloud items=cloud.tags/> }.into_any()
                        }
                        Err(_) => view! { <p class="sidebar-error">"Failed to load tags."</p> }.into_any(),
                    }
                })}
            </Suspense>
        </div>
        <div class="sidebar-section">
            <h3 class="sidebar-heading">"About"</h3>
            <p class="sidebar-about">
                "A personal blog about software engineering, technology, and more."
            </p>
            <a href="/pages/about" class="sidebar-about-link">"Read more"</a>
        </div>
    }
}
