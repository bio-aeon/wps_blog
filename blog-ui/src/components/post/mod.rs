pub mod post_card;
pub mod post_content;
pub mod post_meta;

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
