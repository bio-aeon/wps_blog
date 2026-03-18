pub mod post_card;
pub mod post_content;
pub mod post_meta;
pub mod toc;

pub use post_card::PostCard;
pub use post_content::PostContent;
pub use post_meta::PostMeta;

use chrono::{DateTime, FixedOffset};

fn parse_date(iso_date: &str) -> Option<DateTime<FixedOffset>> {
    let date_str = iso_date
        .find('[')
        .map(|i| &iso_date[..i])
        .unwrap_or(iso_date);

    DateTime::parse_from_rfc3339(date_str)
        .or_else(|_| DateTime::parse_from_str(date_str, "%Y-%m-%dT%H:%M:%S%.f%:z"))
        .ok()
}

/// Formats an ISO 8601 date string into "Month DD, YYYY" display format.
/// Returns the original string if parsing fails.
pub fn format_date(iso_date: &str) -> String {
    parse_date(iso_date)
        .map(|dt| dt.format("%B %d, %Y").to_string())
        .unwrap_or_else(|| iso_date.to_string())
}

/// Formats an ISO 8601 date string into "Mon DD, YYYY" short display format.
pub fn format_date_short(iso_date: &str) -> String {
    parse_date(iso_date)
        .map(|dt| dt.format("%b %-d, %Y").to_string())
        .unwrap_or_else(|| iso_date.to_string())
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
