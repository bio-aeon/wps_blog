use leptos::prelude::*;

/// Minimum number of headings to show a table of contents.
const MIN_HEADINGS_FOR_TOC: usize = 3;

/// Represents a heading in the table of contents.
#[derive(Clone, Debug, PartialEq)]
pub struct TocEntry {
    pub id: String,
    pub text: String,
    pub level: u8,
}

/// Extracts h2–h4 headings from HTML content for table of contents.
/// Returns empty vec if fewer than 3 headings found.
pub fn extract_headings(html_content: &str) -> Vec<TocEntry> {
    let mut entries = Vec::new();
    let mut remaining = html_content;

    while let Some(h_start) = remaining.find("<h") {
        remaining = &remaining[h_start..];

        let level = remaining.as_bytes().get(2).copied();
        let level = match level {
            Some(b'2') => 2u8,
            Some(b'3') => 3,
            Some(b'4') => 4,
            _ => {
                remaining = &remaining[2..];
                continue;
            }
        };

        let tag = format!("</h{}>", level);
        let open_end = remaining.find('>');
        let close_start = remaining.find(&tag);

        if let (Some(oe), Some(cs)) = (open_end, close_start) {
            let text_html = &remaining[oe + 1..cs];
            let text = super::strip_html_tags(text_html).trim().to_string();

            if !text.is_empty() {
                let id = slugify(&text);
                entries.push(TocEntry { id, text, level });
            }

            remaining = &remaining[cs + tag.len()..];
        } else {
            remaining = &remaining[2..];
        }
    }

    if entries.len() < MIN_HEADINGS_FOR_TOC {
        return Vec::new();
    }

    entries
}

/// Converts text to a URL-friendly slug.
pub fn slugify(text: &str) -> String {
    text.to_lowercase()
        .chars()
        .map(|c| {
            if c.is_alphanumeric() {
                c
            } else if c == ' ' || c == '-' || c == '_' {
                '-'
            } else {
                '\0'
            }
        })
        .filter(|c| *c != '\0')
        .collect::<String>()
        .split('-')
        .filter(|s| !s.is_empty())
        .collect::<Vec<_>>()
        .join("-")
}

/// Adds `id` attributes to h2/h3/h4 headings in HTML content for anchor linking.
pub fn inject_heading_ids(html: &str) -> String {
    let mut result = String::with_capacity(html.len() + 256);
    let mut remaining = html;

    while let Some(h_start) = remaining.find("<h") {
        result.push_str(&remaining[..h_start]);
        remaining = &remaining[h_start..];

        let level = remaining.as_bytes().get(2).copied();
        let is_target = matches!(level, Some(b'2') | Some(b'3') | Some(b'4'));

        if is_target {
            let level_char = level.unwrap() as char;
            let tag_close = format!("</h{}>", level_char);

            if let (Some(open_end), Some(close_start)) =
                (remaining.find('>'), remaining.find(&tag_close))
            {
                let tag_attrs = &remaining[..open_end];

                if tag_attrs.contains("id=") {
                    // Keep existing — copy whole tag through
                    let end = close_start + tag_close.len();
                    result.push_str(&remaining[..end]);
                    remaining = &remaining[end..];
                } else {
                    let text_html = &remaining[open_end + 1..close_start];
                    let text = super::strip_html_tags(text_html).trim().to_string();
                    let id = slugify(&text);

                    // Write opening tag with injected id
                    result.push_str(&format!("<h{} id=\"{}\"", level_char, id));
                    // Copy rest of opening tag (after <hN) through closing tag
                    let end = close_start + tag_close.len();
                    result.push_str(&remaining[3..end]);
                    remaining = &remaining[end..];
                }
                continue;
            }
        }

        // Not a target heading or couldn't parse — copy <h and move on
        result.push_str(&remaining[..2]);
        remaining = &remaining[2..];
    }

    result.push_str(remaining);
    result
}

#[component]
pub fn TableOfContents(entries: Vec<TocEntry>) -> impl IntoView {
    if entries.is_empty() {
        return view! {}.into_any();
    }

    view! {
        <nav class="toc" aria-label="Table of contents">
            <h2 class="toc-title">"Contents"</h2>
            <ul class="toc-list">
                {entries
                    .into_iter()
                    .map(|entry| {
                        let indent_class = format!("toc-level-{}", entry.level);
                        let href = format!("#{}", entry.id);
                        view! {
                            <li class=indent_class>
                                <a href=href>{entry.text}</a>
                            </li>
                        }
                    })
                    .collect_view()}
            </ul>
        </nav>
    }
    .into_any()
}
