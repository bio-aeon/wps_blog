use crate::api::types::TagResult;
use crate::i18n::{lang_href, use_language};
use leptos::prelude::*;

fn tag_color_class(slug: &str) -> &'static str {
    match slug {
        "rust" | "leptos" | "wasm" | "wasm-bindgen" => "tag-rust",
        "scala" | "http4s" | "doobie" => "tag-scala",
        "fp" | "cats-effect" | "functional-programming" => "tag-fp",
        "web" | "ssr" | "css" | "html" | "javascript" => "tag-web",
        "postgresql" | "databases" | "sql" | "redis" => "tag-db",
        _ => "",
    }
}

#[component]
pub fn TagBadge(tag: TagResult) -> impl IntoView {
    let lang = use_language();
    let href = lang_href(&lang, &format!("/tags/{}", tag.slug));
    let color_class = tag_color_class(&tag.slug);
    let class = if color_class.is_empty() {
        "tag-badge".to_string()
    } else {
        format!("tag-badge {color_class}")
    };

    view! {
        <a href=href class=class>{tag.name}</a>
    }
}
