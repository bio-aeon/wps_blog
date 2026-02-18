use leptos::prelude::*;

#[component]
pub fn ErrorDisplay(
    title: String,
    message: String,
    #[prop(optional)] retry: Option<Callback<()>>,
    #[prop(optional)] back_url: Option<String>,
    #[prop(optional)] back_label: Option<String>,
) -> impl IntoView {
    view! {
        <div class="error-display">
            <h2 class="error-display-title">{title}</h2>
            <p class="error-display-message">{message}</p>
            <div class="error-display-actions">
                {retry
                    .map(|cb| {
                        view! {
                            <button class="error-retry-btn" on:click=move |_| cb.run(())>
                                "Try again"
                            </button>
                        }
                    })}
                {back_url
                    .map(|url| {
                        let label = back_label.unwrap_or_else(|| "Go back".to_string());
                        view! {
                            <a href=url class="back-link">{label}</a>
                        }
                    })}
            </div>
        </div>
    }
}
