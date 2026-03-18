use super::{get_translations, Translations, DEFAULT_LANG};
use leptos::prelude::*;

#[derive(Clone, Copy, Debug)]
pub struct LanguageContext {
    pub lang: RwSignal<String>,
}

pub fn provide_language_context(lang: String) {
    provide_context(LanguageContext {
        lang: RwSignal::new(lang),
    });
}

pub fn use_language_signal() -> RwSignal<String> {
    use_context::<LanguageContext>()
        .map(|ctx| ctx.lang)
        .unwrap_or_else(|| RwSignal::new(DEFAULT_LANG.to_string()))
}

pub fn use_language() -> String {
    use_language_signal().get()
}

pub fn use_translations() -> &'static Translations {
    let lang = use_language();
    get_translations(&lang)
}

/// Builds a language-prefixed path.
pub fn lang_href(lang: &str, path: &str) -> String {
    if path.starts_with('/') {
        format!("/{}{}", lang, path)
    } else {
        format!("/{}/{}", lang, path)
    }
}
