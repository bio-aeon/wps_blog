use crate::api::create_comment;
use leptos::prelude::*;

const MAX_NAME_LEN: usize = 255;
const MAX_EMAIL_LEN: usize = 255;
const MAX_TEXT_LEN: usize = 10000;

#[component]
pub fn CommentForm(
    post_id: i32,
    #[prop(optional)] parent_id: Option<i32>,
    #[prop(optional)] on_success: Option<Callback<()>>,
) -> impl IntoView {
    let (name, set_name) = signal(String::new());
    let (email, set_email) = signal(String::new());
    let (text, set_text) = signal(String::new());
    let (submitting, set_submitting) = signal(false);
    let (error_msg, set_error_msg) = signal(Option::<String>::None);
    let (name_error, set_name_error) = signal(Option::<String>::None);
    let (email_error, set_email_error) = signal(Option::<String>::None);
    let (text_error, set_text_error) = signal(Option::<String>::None);

    let validate = move || -> bool {
        let mut valid = true;

        let n = name.get();
        if n.trim().is_empty() {
            set_name_error.set(Some("Name is required".into()));
            valid = false;
        } else if n.len() > MAX_NAME_LEN {
            set_name_error.set(Some(format!("Name must be at most {} characters", MAX_NAME_LEN)));
            valid = false;
        } else {
            set_name_error.set(None);
        }

        let e = email.get();
        if e.trim().is_empty() {
            set_email_error.set(Some("Email is required".into()));
            valid = false;
        } else if e.len() > MAX_EMAIL_LEN {
            set_email_error
                .set(Some(format!("Email must be at most {} characters", MAX_EMAIL_LEN)));
            valid = false;
        } else if !e.contains('@') || !e.contains('.') {
            set_email_error.set(Some("Please enter a valid email".into()));
            valid = false;
        } else {
            set_email_error.set(None);
        }

        let t = text.get();
        if t.trim().is_empty() {
            set_text_error.set(Some("Comment text is required".into()));
            valid = false;
        } else if t.len() > MAX_TEXT_LEN {
            set_text_error
                .set(Some(format!("Comment must be at most {} characters", MAX_TEXT_LEN)));
            valid = false;
        } else {
            set_text_error.set(None);
        }

        valid
    };

    let on_success = on_success.clone();
    let handle_submit = move |ev: leptos::ev::SubmitEvent| {
        ev.prevent_default();
        if !validate() || submitting.get() {
            return;
        }

        set_submitting.set(true);
        set_error_msg.set(None);

        let n = name.get();
        let e = email.get();
        let t = text.get();
        let pid = parent_id;
        let on_success = on_success.clone();

        leptos::task::spawn_local(async move {
            match create_comment(post_id, n, e, t, pid).await {
                Ok(_) => {
                    set_name.set(String::new());
                    set_email.set(String::new());
                    set_text.set(String::new());
                    set_submitting.set(false);
                    if let Some(cb) = on_success {
                        cb.run(());
                    }
                }
                Err(err) => {
                    set_error_msg.set(Some(err.to_string()));
                    set_submitting.set(false);
                }
            }
        });
    };

    view! {
        <form class="comment-form" on:submit=handle_submit>
            {move || {
                error_msg
                    .get()
                    .map(|msg| {
                        view! { <div class="comment-form-error">{msg}</div> }
                    })
            }}
            <div class="form-group">
                <label for="comment-name">"Name"</label>
                <input
                    type="text"
                    id="comment-name"
                    placeholder="Your name"
                    maxlength=MAX_NAME_LEN.to_string()
                    prop:value=move || name.get()
                    on:input=move |ev| set_name.set(event_target_value(&ev))
                />
                {move || {
                    name_error.get().map(|e| view! { <span class="field-error">{e}</span> })
                }}
            </div>
            <div class="form-group">
                <label for="comment-email">"Email"</label>
                <input
                    type="email"
                    id="comment-email"
                    placeholder="your@email.com"
                    maxlength=MAX_EMAIL_LEN.to_string()
                    prop:value=move || email.get()
                    on:input=move |ev| set_email.set(event_target_value(&ev))
                />
                {move || {
                    email_error.get().map(|e| view! { <span class="field-error">{e}</span> })
                }}
            </div>
            <div class="form-group">
                <label for="comment-text">"Comment"</label>
                <textarea
                    id="comment-text"
                    placeholder="Write your comment..."
                    maxlength=MAX_TEXT_LEN.to_string()
                    rows="4"
                    prop:value=move || text.get()
                    on:input=move |ev| set_text.set(event_target_value(&ev))
                ></textarea>
                {move || {
                    text_error.get().map(|e| view! { <span class="field-error">{e}</span> })
                }}
            </div>
            <button type="submit" class="comment-submit" disabled=move || submitting.get()>
                {move || if submitting.get() { "Posting..." } else { "Post Comment" }}
            </button>
        </form>
    }
}
