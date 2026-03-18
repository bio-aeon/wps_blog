use crate::i18n::{lang_href, use_language, use_translations};
use leptos::prelude::*;
use leptos_meta::Title;

#[component]
pub fn NotFoundPage() -> impl IntoView {
    #[cfg(feature = "ssr")]
    {
        let resp = expect_context::<leptos_actix::ResponseOptions>();
        resp.set_status(actix_web::http::StatusCode::NOT_FOUND);
    }

    let lang = use_language();
    let t = use_translations();
    let home_href = lang_href(&lang, "/");

    view! {
        <Title text=format!("{} - WPS Blog", t.common.not_found)/>
        <div class="not-found-page">
            <h1>{t.common.not_found}</h1>
            <p>{t.common.not_found_message}</p>
            <a href=home_href class="back-link">{t.common.back_home}</a>
        </div>
    }
}
