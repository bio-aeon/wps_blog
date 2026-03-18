use crate::api::{get_recent_posts, get_tag_cloud};
use crate::components::common::SidebarSkeleton;
use crate::components::post::format_date_short;
use crate::components::tag::TagCloud;
use crate::i18n::{lang_href, use_language, use_translations};
use leptos::prelude::*;

const RECENT_POSTS_COUNT: i32 = 5;

#[component]
pub fn Sidebar() -> impl IntoView {
    let lang = use_language();
    let t = use_translations();
    let recent_posts = Resource::new(
        {
            let lang = lang.clone();
            move || lang.clone()
        },
        |lang| get_recent_posts(lang, RECENT_POSTS_COUNT),
    );
    let tag_cloud = Resource::new(
        {
            let lang = lang.clone();
            move || lang.clone()
        },
        |lang| get_tag_cloud(lang),
    );

    let about_href = lang_href(&lang, "/about");

    view! {
        <div class="sidebar-section">
            <h3 class="sidebar-heading">{t.common.recent_posts}</h3>
            <Suspense fallback=move || view! { <SidebarSkeleton/> }>
                {move || {
                    let lang = lang.clone();
                    Suspend::new(async move {
                        match recent_posts.await {
                            Ok(posts) => {
                                view! {
                                    <ul class="recent-posts-list">
                                        {posts
                                            .into_iter()
                                            .map(|post| {
                                                let href = lang_href(&lang, &format!("/posts/{}", post.id));
                                                let date = format_date_short(&post.created_at);
                                                view! {
                                                    <li>
                                                        <a href=href>{post.name}</a>
                                                        <div class="recent-post-date">{date}</div>
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
                    })
                }}
            </Suspense>
        </div>
        <div class="sidebar-section">
            <h3 class="sidebar-heading">{t.nav.tags}</h3>
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
            <h3 class="sidebar-heading">{t.nav.about}</h3>
            <p class="sidebar-about">
                "A personal blog about software engineering, technology, and more."
            </p>
            <a href=about_href class="sidebar-about-link">{t.post.read_more}</a>
        </div>
    }
}
