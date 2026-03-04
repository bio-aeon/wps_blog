pub mod post_card;
pub mod post_content;
pub mod post_meta;
pub mod toc;

pub use post_card::PostCard;
pub use post_content::PostContent;
pub use post_meta::PostMeta;

use chrono::{DateTime, FixedOffset};

/// Formats an ISO 8601 date string into "Month DD, YYYY" display format.
/// Returns the original string if parsing fails.
pub fn format_date(iso_date: &str) -> String {
    DateTime::parse_from_rfc3339(iso_date)
        .or_else(|_| DateTime::parse_from_str(iso_date, "%Y-%m-%dT%H:%M:%S%.f%:z"))
        .map(|dt: DateTime<FixedOffset>| dt.format("%B %d, %Y").to_string())
        .unwrap_or_else(|_| iso_date.to_string())
}

/// Average words per minute for reading speed estimation.
const WORDS_PER_MINUTE: usize = 200;

/// Estimates reading time in minutes from HTML content.
/// Strips HTML tags and counts words. Returns minimum 1 minute.
pub fn estimate_reading_time(html_content: &str) -> usize {
    let text = strip_html_tags(html_content);
    let word_count = text.split_whitespace().count();
    (word_count / WORDS_PER_MINUTE).max(1)
}

/// Strips HTML tags from a string, returning only text content.
pub fn strip_html_tags(html: &str) -> String {
    let mut result = String::with_capacity(html.len());
    let mut in_tag = false;
    for c in html.chars() {
        if c == '<' {
            in_tag = true;
        } else if c == '>' {
            in_tag = false;
        } else if !in_tag {
            result.push(c);
        }
    }
    result
}
