static ENGLISH_MONTHS: [&str; 12] = [
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
];

static RUSSIAN_MONTHS: [&str; 12] = [
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря",
];

static GREEK_MONTHS: [&str; 12] = [
    "Ιανουαρίου", "Φεβρουαρίου", "Μαρτίου", "Απριλίου", "Μαΐου", "Ιουνίου",
    "Ιουλίου", "Αυγούστου", "Σεπτεμβρίου", "Οκτωβρίου", "Νοεμβρίου", "Δεκεμβρίου",
];

/// Format ISO 8601 date string for the given language.
/// Input: "2024-01-15T10:00:00Z" or similar ISO format.
/// Output: "January 15, 2024" (en) / "15 января 2024" (ru) / "15 Ιανουαρίου 2024" (el)
pub fn format_date_localized(iso_date: &str, lang: &str) -> String {
    let date_part = iso_date.split('T').next().unwrap_or(iso_date);
    let parts: Vec<&str> = date_part.split('-').collect();
    if parts.len() < 3 {
        return iso_date.to_string();
    }

    let year = parts[0];
    let month_idx: usize = parts[1].parse::<usize>().unwrap_or(1).saturating_sub(1).min(11);
    let day: u32 = parts[2].parse().unwrap_or(1);

    match lang {
        "ru" => format!("{} {} {}", day, RUSSIAN_MONTHS[month_idx], year),
        "el" => format!("{} {} {}", day, GREEK_MONTHS[month_idx], year),
        _ => format!("{} {}, {}", ENGLISH_MONTHS[month_idx], day, year),
    }
}
