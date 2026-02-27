use crate::components::contact::ContactForm;
use leptos::prelude::*;
use leptos_meta::Title;

#[component]
pub fn ContactPage() -> impl IntoView {
    view! {
        <Title text="Contact - WPS Blog"/>
        <article class="contact-page">
            <h1>"Get in Touch"</h1>
            <p class="contact-intro">
                "Have a question or want to connect? Fill out the form below."
            </p>
            <ContactForm/>
        </article>
    }
}
