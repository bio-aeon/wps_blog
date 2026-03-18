use crate::api::get_tags;
use crate::components::common::{ErrorDisplay, TagListSkeleton};
use crate::i18n::{lang_href, use_language, use_translations};
use leptos::prelude::*;
use leptos_meta::Title;

#[component]
pub fn TagListPage() -> impl IntoView {
    let lang = use_language();
    let t = use_translations();
    let tags_resource = Resource::new(
        {
            let lang = lang.clone();
            move || lang.clone()
        },
        |lang| get_tags(lang),
    );

    view! {
        <Title text=format!("{} - WPS Blog", t.tag.all_tags)/>
        <h1>{t.tag.all_tags}</h1>
        <Suspense fallback=move || {
            view! { <TagListSkeleton/> }
        }>
            {move || {
                let lang = lang.clone();
                let back_label = t.common.back_home;
                Suspend::new(async move {
                    match tags_resource.await {
                        Ok(result) => {
                            view! {
                                <div class="tag-list">
                                    {result
                                        .items
                                        .into_iter()
                                        .map(|tag| {
                                            let href = lang_href(&lang, &format!("/tags/{}", tag.slug));
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
                                <ErrorDisplay
                                    title="Failed to load tags".to_string()
                                    message=e.to_string()
                                    back_url="/".to_string()
                                    back_label=back_label.to_string()
                                />
                            }
                            .into_any()
                        }
                    }
                })
            }}
        </Suspense>
    }
}
