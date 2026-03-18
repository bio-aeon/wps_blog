pub mod context;
pub mod en;
pub mod el;
pub mod formatting;
pub mod plurals;
pub mod ru;

pub use context::*;
pub use formatting::*;
pub use plurals::*;

pub struct Translations {
    pub nav: NavStrings,
    pub post: PostStrings,
    pub comment: CommentStrings,
    pub tag: TagStrings,
    pub search: SearchStrings,
    pub common: CommonStrings,
    pub contact: ContactStrings,
    pub about: AboutStrings,
    pub footer: FooterStrings,
}

pub struct NavStrings {
    pub home: &'static str,
    pub posts: &'static str,
    pub tags: &'static str,
    pub about: &'static str,
    pub contact: &'static str,
    pub search_placeholder: &'static str,
}

pub struct PostStrings {
    pub read_more: &'static str,
    pub published_on: &'static str,
    pub reading_time: &'static str,
    pub views: &'static str,
    pub table_of_contents: &'static str,
    pub back_to_posts: &'static str,
}

pub struct CommentStrings {
    pub title: &'static str,
    pub leave_comment: &'static str,
    pub name_label: &'static str,
    pub email_label: &'static str,
    pub text_label: &'static str,
    pub submit: &'static str,
    pub reply: &'static str,
    pub no_comments: &'static str,
}

pub struct TagStrings {
    pub all_tags: &'static str,
    pub posts_tagged: &'static str,
}

pub struct SearchStrings {
    pub title: &'static str,
    pub placeholder: &'static str,
    pub results_for: &'static str,
    pub no_results: &'static str,
}

pub struct CommonStrings {
    pub loading: &'static str,
    pub error_title: &'static str,
    pub error_message: &'static str,
    pub not_found: &'static str,
    pub not_found_message: &'static str,
    pub retry: &'static str,
    pub back_home: &'static str,
    pub page: &'static str,
    pub previous: &'static str,
    pub next: &'static str,
    pub recent_posts: &'static str,
    pub view_all_posts: &'static str,
}

pub struct ContactStrings {
    pub title: &'static str,
    pub name_label: &'static str,
    pub email_label: &'static str,
    pub subject_label: &'static str,
    pub message_label: &'static str,
    pub submit: &'static str,
    pub success: &'static str,
}

pub struct AboutStrings {
    pub title: &'static str,
    pub skills: &'static str,
    pub experience: &'static str,
    pub present: &'static str,
}

pub struct FooterStrings {
    pub copyright: &'static str,
    pub rss_feed: &'static str,
}

pub const SUPPORTED_LANGS: [&str; 3] = ["en", "ru", "el"];
pub const DEFAULT_LANG: &str = "en";

pub fn get_translations(lang: &str) -> &'static Translations {
    match lang {
        "ru" => &ru::TRANSLATIONS,
        "el" => &el::TRANSLATIONS,
        _ => &en::TRANSLATIONS,
    }
}

pub fn is_supported_lang(lang: &str) -> bool {
    SUPPORTED_LANGS.contains(&lang)
}
