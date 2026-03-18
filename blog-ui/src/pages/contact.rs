use crate::components::contact::ContactForm;
use crate::i18n::use_translations;
use leptos::prelude::*;
use leptos_meta::Title;

#[component]
pub fn ContactPage() -> impl IntoView {
    let t = use_translations();

    view! {
        <Title text=format!("{} - WPS Blog", t.contact.title)/>
        <article class="contact-page">
            <h1>{t.contact.title}</h1>
            <p class="contact-intro">
                "Have a question or want to connect? Fill out the form below."
            </p>
            <ContactForm/>
        </article>
    }
}
