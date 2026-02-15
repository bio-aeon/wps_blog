use crate::api::get_tags;
use leptos::prelude::*;
use leptos_meta::Title;

#[component]
pub fn TagListPage() -> impl IntoView {
    let tags_resource = Resource::new(|| (), |_| get_tags());

    view! {
        <Title text="Tags - WPS Blog"/>
        <h1>"Tags"</h1>
        <Suspense fallback=move || {
            view! { <TagListSkeleton/> }
        }>
            {move || Suspend::new(async move {
                match tags_resource.await {
                    Ok(result) => {
                        view! {
                            <div class="tag-list">
                                {result
                                    .items
                                    .into_iter()
                                    .map(|tag| {
                                        let href = format!("/tags/{}", tag.slug);
                                        let count_text = format!(
                                            "{} {}",
                                            tag.post_count,
                                            if tag.post_count == 1 { "post" } else { "posts" },
                                        );
                                        view! {
                                            <a href=href class="tag-list-item">
                                                <span class="tag-name">{tag.name}</span>
                                                <span class="tag-count">{count_text}</span>
                                            </a>
                                        }
                                    })
                                    .collect_view()}
                            </div>
                        }
                        .into_any()
                    }
                    Err(e) => {
                        view! {
                            <div class="error-message">
                                <p>"Failed to load tags: " {e.to_string()}</p>
                            </div>
                        }
                        .into_any()
                    }
                }
            })}
        </Suspense>
    }
}

#[component]
fn TagListSkeleton() -> impl IntoView {
    view! {
        <div class="tag-list-skeleton">
            {(0..5)
                .map(|_| {
                    view! {
                        <div class="skeleton-line tag-item"></div>
                    }
                })
                .collect_view()}
        </div>
    }
}
