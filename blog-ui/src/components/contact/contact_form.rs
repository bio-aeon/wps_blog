use crate::api::submit_contact;
use leptos::prelude::*;

pub const MAX_CONTACT_NAME_LEN: usize = 255;
pub const MAX_CONTACT_EMAIL_LEN: usize = 255;
pub const MAX_CONTACT_SUBJECT_LEN: usize = 500;
pub const MAX_CONTACT_MESSAGE_LEN: usize = 5000;
pub const MIN_CONTACT_SUBJECT_LEN: usize = 3;
pub const MIN_CONTACT_MESSAGE_LEN: usize = 10;

/// Validates contact form fields. Returns Ok(()) or Err(HashMap<field, error>).
pub fn validate_contact_fields(
    name: &str,
    email: &str,
    subject: &str,
    message: &str,
) -> Result<(), std::collections::HashMap<String, String>> {
    let mut errors = std::collections::HashMap::new();
    let name = name.trim();
    let email = email.trim();
    let subject = subject.trim();
    let message = message.trim();

    if name.is_empty() {
        errors.insert("name".into(), "Name is required".into());
    } else if name.len() > MAX_CONTACT_NAME_LEN {
        errors.insert(
            "name".into(),
            format!("Name must be at most {} characters", MAX_CONTACT_NAME_LEN),
        );
    }

    if email.is_empty() {
        errors.insert("email".into(), "Email is required".into());
    } else if email.len() > MAX_CONTACT_EMAIL_LEN {
        errors.insert(
            "email".into(),
            format!("Email must be at most {} characters", MAX_CONTACT_EMAIL_LEN),
        );
    } else if !email.contains('@') || !email.contains('.') {
        errors.insert("email".into(), "Invalid email address".into());
    }

    if subject.is_empty() {
        errors.insert("subject".into(), "Subject is required".into());
    } else if subject.len() < MIN_CONTACT_SUBJECT_LEN {
        errors.insert(
            "subject".into(),
            format!(
                "Subject must be at least {} characters",
                MIN_CONTACT_SUBJECT_LEN
            ),
        );
    } else if subject.len() > MAX_CONTACT_SUBJECT_LEN {
        errors.insert(
            "subject".into(),
            format!(
                "Subject must be at most {} characters",
                MAX_CONTACT_SUBJECT_LEN
            ),
        );
    }

    if message.is_empty() {
        errors.insert("message".into(), "Message is required".into());
    } else if message.len() < MIN_CONTACT_MESSAGE_LEN {
        errors.insert(
            "message".into(),
            format!(
                "Message must be at least {} characters",
                MIN_CONTACT_MESSAGE_LEN
            ),
        );
    } else if message.len() > MAX_CONTACT_MESSAGE_LEN {
        errors.insert(
            "message".into(),
            format!(
                "Message must be at most {} characters",
                MAX_CONTACT_MESSAGE_LEN
            ),
        );
    }

    if errors.is_empty() {
        Ok(())
    } else {
        Err(errors)
    }
}

#[component]
pub fn ContactForm() -> impl IntoView {
    let (name, set_name) = signal(String::new());
    let (email, set_email) = signal(String::new());
    let (subject, set_subject) = signal(String::new());
    let (message, set_message) = signal(String::new());
    let (honeypot, set_honeypot) = signal(String::new());
    let (errors, set_errors) =
        signal(std::collections::HashMap::<String, String>::new());
    let (submitting, set_submitting) = signal(false);
    let (success_message, set_success_message) = signal(Option::<String>::None);

    let on_submit = move |ev: leptos::ev::SubmitEvent| {
        ev.prevent_default();
        set_errors.set(std::collections::HashMap::new());
        set_success_message.set(None);

        if let Err(errs) = validate_contact_fields(
            &name.get(),
            &email.get(),
            &subject.get(),
            &message.get(),
        ) {
            set_errors.set(errs);
            return;
        }

        // Honeypot check — if filled, silently "succeed"
        if !honeypot.get().is_empty() {
            set_success_message.set(Some("Thank you for your message!".into()));
            return;
        }

        set_submitting.set(true);
        let n = name.get();
        let e = email.get();
        let s = subject.get();
        let m = message.get();

        leptos::task::spawn_local(async move {
            match submit_contact(n, e, s, m).await {
                Ok(resp) => {
                    set_success_message.set(Some(resp.message));
                    set_name.set(String::new());
                    set_email.set(String::new());
                    set_subject.set(String::new());
                    set_message.set(String::new());
                }
                Err(e) => {
                    let mut errs = std::collections::HashMap::new();
                    errs.insert("form".into(), e.to_string());
                    set_errors.set(errs);
                }
            }
            set_submitting.set(false);
        });
    };

    let field_error = move |field: &'static str| {
        let errs = errors.get();
        errs.get(field).cloned()
    };

    view! {
        {move || {
            success_message
                .get()
                .map(|msg| {
                    view! { <div class="contact-success">{msg}</div> }
                })
        }}
        {move || {
            let errs = errors.get();
            errs.get("form")
                .map(|msg| {
                    view! { <div class="contact-form-error">{msg.clone()}</div> }
                })
        }}
        <form class="contact-form" on:submit=on_submit>
            // Honeypot field — hidden from users, bots fill it
            <div class="contact-honeypot" aria-hidden="true">
                <input
                    type="text"
                    name="website"
                    tabindex="-1"
                    autocomplete="off"
                    prop:value=move || honeypot.get()
                    on:input=move |ev| set_honeypot.set(event_target_value(&ev))
                />
            </div>
            <div class="form-group">
                <label for="contact-name">"Name"</label>
                <input
                    type="text"
                    id="contact-name"
                    placeholder="Your name"
                    required
                    aria-required="true"
                    maxlength=MAX_CONTACT_NAME_LEN.to_string()
                    prop:value=move || name.get()
                    on:input=move |ev| set_name.set(event_target_value(&ev))
                />
                {move || {
                    field_error("name")
                        .map(|e| view! { <span class="field-error">{e}</span> })
                }}
            </div>
            <div class="form-group">
                <label for="contact-email">"Email"</label>
                <input
                    type="email"
                    id="contact-email"
                    placeholder="your@email.com"
                    required
                    aria-required="true"
                    maxlength=MAX_CONTACT_EMAIL_LEN.to_string()
                    prop:value=move || email.get()
                    on:input=move |ev| set_email.set(event_target_value(&ev))
                />
                {move || {
                    field_error("email")
                        .map(|e| view! { <span class="field-error">{e}</span> })
                }}
            </div>
            <div class="form-group">
                <label for="contact-subject">"Subject"</label>
                <input
                    type="text"
                    id="contact-subject"
                    placeholder="Subject"
                    required
                    aria-required="true"
                    maxlength=MAX_CONTACT_SUBJECT_LEN.to_string()
                    prop:value=move || subject.get()
                    on:input=move |ev| set_subject.set(event_target_value(&ev))
                />
                {move || {
                    field_error("subject")
                        .map(|e| view! { <span class="field-error">{e}</span> })
                }}
            </div>
            <div class="form-group">
                <label for="contact-message">"Message"</label>
                <textarea
                    id="contact-message"
                    placeholder="Your message..."
                    required
                    aria-required="true"
                    maxlength=MAX_CONTACT_MESSAGE_LEN.to_string()
                    rows="6"
                    prop:value=move || message.get()
                    on:input=move |ev| set_message.set(event_target_value(&ev))
                ></textarea>
                {move || {
                    field_error("message")
                        .map(|e| view! { <span class="field-error">{e}</span> })
                }}
            </div>
            <button type="submit" class="contact-submit" disabled=move || submitting.get()>
                {move || if submitting.get() { "Sending..." } else { "Send Message" }}
            </button>
        </form>
    }
}
