use crate::i18n::{use_language_signal, SUPPORTED_LANGS};
use leptos::prelude::*;
use leptos_router::hooks::use_location;

#[component]
pub fn LanguageSwitcher() -> impl IntoView {
    let lang_signal = use_language_signal();
    let location = use_location();

    let languages: Vec<(&'static str, &'static str)> =
        SUPPORTED_LANGS.iter().map(|&code| {
            let label = match code {
                "en" => "EN",
                "ru" => "RU",
                "el" => "EL",
                _ => code,
            };
            (code, label)
        }).collect();

    view! {
        <div class="language-switcher" role="navigation" aria-label="Language">
            {languages.into_iter().map(|(code, label)| {
                let location = location.clone();
                let href = move || {
                    let pathname = location.pathname.get();
                    let current = lang_signal.get();
                    let lang_prefix = format!("/{}/", current);
                    let lang_only = format!("/{}", current);
                    if pathname.starts_with(&lang_prefix) {
                        format!("/{}/{}", code, &pathname[lang_prefix.len()..])
                    } else if pathname == lang_only {
                        format!("/{}/", code)
                    } else {
                        format!("/{}/", code)
                    }
                };
                view! {
                    <a
                        href=href
                        class:active=move || lang_signal.get() == code
                        aria-current=move || if lang_signal.get() == code { Some("true") } else { None }
                        target="_top"
                    >
                        {label}
                    </a>
                }
            }).collect_view()}
        </div>
    }
}
