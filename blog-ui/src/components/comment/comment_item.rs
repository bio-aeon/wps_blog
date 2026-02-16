use crate::api::rate_comment;
use crate::api::types::CommentResult;
use crate::components::post::format_date;
use super::CommentForm;
use leptos::prelude::*;

const MAX_NESTING_DEPTH: u32 = 5;

#[component]
pub fn CommentItem(
    comment: CommentResult,
    post_id: i32,
    depth: u32,
    on_comment_added: Callback<()>,
) -> impl IntoView {
    let (show_reply_form, set_show_reply_form) = signal(false);
    let (rating, set_rating) = signal(comment.rating);
    let (voted, set_voted) = signal(false);

    let comment_id = comment.id;
    let date_display = format_date(&comment.created_at);
    let replies = comment.replies;
    let indent_class = format!("comment-depth-{}", depth.min(MAX_NESTING_DEPTH));

    let handle_upvote = move |_| {
        if voted.get() {
            return;
        }
        set_voted.set(true);
        set_rating.update(|v| *v += 1);
        leptos::task::spawn_local(async move {
            if rate_comment(comment_id, true).await.is_err() {
                set_rating.update(|v| *v -= 1);
                set_voted.set(false);
            }
        });
    };

    let handle_downvote = move |_| {
        if voted.get() {
            return;
        }
        set_voted.set(true);
        set_rating.update(|v| *v -= 1);
        leptos::task::spawn_local(async move {
            if rate_comment(comment_id, false).await.is_err() {
                set_rating.update(|v| *v += 1);
                set_voted.set(false);
            }
        });
    };

    let toggle_reply = move |_| {
        set_show_reply_form.set(!show_reply_form.get());
    };

    let on_reply_success = {
        let on_comment_added = on_comment_added.clone();
        Callback::new(move |_: ()| {
            set_show_reply_form.set(false);
            on_comment_added.run(());
        })
    };

    view! {
        <div class=format!("comment-item {}", indent_class)>
            <div class="comment-header">
                <span class="comment-author">{comment.name}</span>
                <time class="comment-date" datetime=comment.created_at>{date_display}</time>
            </div>
            <div class="comment-body">{comment.text}</div>
            <div class="comment-actions">
                <div class="comment-rating">
                    <button
                        class="rate-btn upvote"
                        on:click=handle_upvote
                        disabled=move || voted.get()
                        title="Upvote"
                    >
                        "+"
                    </button>
                    <span class="rating-value">{move || rating.get()}</span>
                    <button
                        class="rate-btn downvote"
                        on:click=handle_downvote
                        disabled=move || voted.get()
                        title="Downvote"
                    >
                        "-"
                    </button>
                </div>
                <button class="reply-btn" on:click=toggle_reply>
                    {move || if show_reply_form.get() { "Cancel" } else { "Reply" }}
                </button>
            </div>
            <Show when=move || show_reply_form.get()>
                <CommentForm
                    post_id=post_id
                    parent_id=comment_id
                    on_success=on_reply_success.clone()
                />
            </Show>
            <div class="comment-replies">
                {replies
                    .into_iter()
                    .map(|reply| {
                        view! {
                            <CommentItem
                                comment=reply
                                post_id=post_id
                                depth=depth + 1
                                on_comment_added=on_comment_added.clone()
                            />
                        }
                        .into_any()
                    })
                    .collect_view()}
            </div>
        </div>
    }
    .into_any()
}
