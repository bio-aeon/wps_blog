use crate::api::get_comments;
use super::{CommentForm, CommentItem};
use leptos::prelude::*;

#[component]
pub fn CommentThread(post_id: i32) -> impl IntoView {
    let (refetch_counter, set_refetch_counter) = signal(0u32);

    let comments_resource = Resource::new(
        move || (post_id, refetch_counter.get()),
        |(post_id, _)| async move { get_comments(post_id).await },
    );

    let on_comment_added = Callback::new(move |_: ()| {
        set_refetch_counter.update(|v| *v += 1);
    });

    view! {
        <section class="comment-section">
            <Suspense fallback=move || {
                view! { <p class="comments-loading">"Loading comments..."</p> }
            }>
                {move || {
                    let on_comment_added = on_comment_added.clone();
                    Suspend::new(async move {
                        match comments_resource.await {
                            Ok(result) => {
                                let heading = format!(
                                    "Comments ({})",
                                    result.total,
                                );
                                view! {
                                    <h2 class="comments-heading">{heading}</h2>
                                    <CommentForm
                                        post_id=post_id
                                        on_success=on_comment_added.clone()
                                    />
                                    <div class="comments-list">
                                        {result
                                            .comments
                                            .into_iter()
                                            .map(|comment| {
                                                view! {
                                                    <CommentItem
                                                        comment=comment
                                                        post_id=post_id
                                                        depth=0
                                                        on_comment_added=on_comment_added.clone()
                                                    />
                                                }
                                            })
                                            .collect_view()}
                                    </div>
                                }
                                .into_any()
                            }
                            Err(e) => {
                                view! {
                                    <h2 class="comments-heading">"Comments"</h2>
                                    <p class="comments-error">
                                        "Failed to load comments: " {e.to_string()}
                                    </p>
                                }
                                .into_any()
                            }
                        }
                    })
                }}
            </Suspense>
        </section>
    }
}
