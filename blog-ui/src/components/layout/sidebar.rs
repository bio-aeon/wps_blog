use crate::api::{get_recent_posts, get_tag_cloud};
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
                            view! {
                                <div class="tag-cloud">
                                    {cloud
                                        .tags
                                        .into_iter()
                                        .map(|tag| {
                                            let font_size = format!("{}rem", 0.8 + tag.weight * 1.2);
                                            let href = format!("/tags/{}", tag.slug);
                                            view! {
                                                <a
                                                    href=href
                                                    class="tag-cloud-item"
                                                    style:font-size=font_size
                                                >
                                                    {tag.name}
                                                </a>
                                            }
                                        })
                                        .collect_view()}
                                </div>
                            }
                            .into_any()
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

#[component]
fn SidebarSkeleton() -> impl IntoView {
    view! {
        <div class="sidebar-skeleton">
            <div class="skeleton-line"></div>
            <div class="skeleton-line short"></div>
            <div class="skeleton-line"></div>
        </div>
    }
}
