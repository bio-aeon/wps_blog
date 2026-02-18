use leptos::prelude::*;
use leptos_meta::Title;

#[component]
pub fn NotFoundPage() -> impl IntoView {
    #[cfg(feature = "ssr")]
    {
        let resp = expect_context::<leptos_actix::ResponseOptions>();
        resp.set_status(actix_web::http::StatusCode::NOT_FOUND);
    }

    view! {
        <Title text="404 - Page Not Found - WPS Blog"/>
        <div class="not-found-page">
            <h1>"404 - Page Not Found"</h1>
            <p>"The page you are looking for does not exist."</p>
            <a href="/" class="back-link">"← Back to Home"</a>
        </div>
    }
}
