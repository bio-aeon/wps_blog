use crate::api::get_recent_posts;
use crate::components::common::{ErrorDisplay, PostListSkeleton};
use crate::components::post::PostCard;
use crate::i18n::{lang_href, use_language, use_translations};
use leptos::prelude::*;
use leptos_meta::Title;

const RECENT_POSTS_COUNT: i32 = 5;

#[component]
pub fn HomePage() -> impl IntoView {
    let lang = use_language();
    let t = use_translations();
    let recent_posts = Resource::new(
        {
            let lang = lang.clone();
            move || lang.clone()
        },
        |lang| get_recent_posts(lang, RECENT_POSTS_COUNT),
    );

    let posts_href = lang_href(&lang, "/posts");

    view! {
        <Title text="WPS Blog"/>
        <Suspense fallback=move || {
            view! { <PostListSkeleton count=RECENT_POSTS_COUNT as usize/> }
        }>
            {move || {
                let posts_href = posts_href.clone();
                let view_all = t.common.view_all_posts;
                Suspend::new(async move {
                    match recent_posts.await {
                        Ok(posts) => {
                            view! {
                                <div class="post-list">
                                    {posts
                                        .into_iter()
                                        .map(|post| view! { <PostCard post=post/> })
                                        .collect_view()}
                                </div>
                                <a href=posts_href class="view-all-link">{view_all}</a>
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
                })
            }}
        </Suspense>
    }
}
