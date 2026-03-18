/// Russian plural forms: 1 комментарий, 2 комментария, 5 комментариев
/// Rules: last two digits 11-19 -> many; last digit 1 -> one; 2-4 -> few; else -> many
pub fn pluralize_ru(count: u32, one: &str, few: &str, many: &str) -> String {
    let last_two = count % 100;
    let last_one = count % 10;
    let form = if (11..=19).contains(&last_two) {
        many
    } else if last_one == 1 {
        one
    } else if (2..=4).contains(&last_one) {
        few
    } else {
        many
    };
    format!("{} {}", count, form)
}

/// Simple singular/plural for English and Greek
pub fn pluralize_simple(count: u32, singular: &str, plural: &str) -> String {
    if count == 1 {
        format!("{} {}", count, singular)
    } else {
        format!("{} {}", count, plural)
    }
}

pub fn pluralize_comments(count: u32, lang: &str) -> String {
    match lang {
        "ru" => pluralize_ru(count, "комментарий", "комментария", "комментариев"),
        "el" => pluralize_simple(count, "σχόλιο", "σχόλια"),
        _ => pluralize_simple(count, "comment", "comments"),
    }
}

pub fn pluralize_posts(count: u32, lang: &str) -> String {
    match lang {
        "ru" => pluralize_ru(count, "пост", "поста", "постов"),
        "el" => pluralize_simple(count, "άρθρο", "άρθρα"),
        _ => pluralize_simple(count, "post", "posts"),
    }
}
