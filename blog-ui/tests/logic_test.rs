use blog_ui::components::about::experience_item::format_experience_date;
use blog_ui::components::comment::comment_form::{
    validate_comment_fields, MAX_EMAIL_LEN, MAX_NAME_LEN, MAX_TEXT_LEN,
};
use blog_ui::components::common::pagination::{calculate_pagination, page_url};
use blog_ui::components::contact::contact_form::{
    validate_contact_fields as validate_contact, MAX_CONTACT_EMAIL_LEN, MAX_CONTACT_MESSAGE_LEN,
    MAX_CONTACT_NAME_LEN, MAX_CONTACT_SUBJECT_LEN,
};
use blog_ui::components::post::toc::{extract_headings, inject_heading_ids, slugify, TocEntry};
use blog_ui::components::post::{estimate_reading_time, format_date, strip_html_tags};
use blog_ui::components::tag::tag_cloud::{tag_font_size, MAX_FONT_SIZE_REM, MIN_FONT_SIZE_REM};

// --- format_date ---

#[test]
fn format_date_rfc3339() {
    assert_eq!(format_date("2024-01-15T10:00:00+00:00"), "January 15, 2024");
}

#[test]
fn format_date_with_timezone_offset() {
    assert_eq!(format_date("2024-12-25T18:30:00+03:00"), "December 25, 2024");
}

#[test]
fn format_date_invalid_returns_original() {
    assert_eq!(format_date("not-a-date"), "not-a-date");
}

// --- calculate_pagination ---

#[test]
fn pagination_hidden_for_single_page() {
    assert!(calculate_pagination(1, 5, 10).is_none());
}

#[test]
fn pagination_hidden_for_zero_items() {
    assert!(calculate_pagination(1, 0, 10).is_none());
}

#[test]
fn pagination_two_pages() {
    let calc = calculate_pagination(1, 15, 10).unwrap();
    assert_eq!(calc.total_pages, 2);
    assert_eq!(calc.page_window, vec![1, 2]);
    assert!(!calc.has_prev);
    assert!(calc.has_next);
}

#[test]
fn pagination_middle_of_many_pages() {
    let calc = calculate_pagination(5, 100, 10).unwrap();
    assert_eq!(calc.total_pages, 10);
    assert_eq!(calc.page_window, vec![3, 4, 5, 6, 7]);
    assert!(calc.has_prev);
    assert!(calc.has_next);
}

#[test]
fn pagination_last_page() {
    let calc = calculate_pagination(10, 100, 10).unwrap();
    assert_eq!(calc.page_window, vec![6, 7, 8, 9, 10]);
    assert!(calc.has_prev);
    assert!(!calc.has_next);
}

#[test]
fn pagination_first_page_of_many() {
    let calc = calculate_pagination(1, 100, 10).unwrap();
    assert_eq!(calc.page_window, vec![1, 2, 3, 4, 5]);
    assert!(!calc.has_prev);
    assert!(calc.has_next);
}

#[test]
fn page_url_first_page_has_no_query() {
    assert_eq!(page_url("/posts", 1), "/posts");
}

#[test]
fn page_url_other_pages_have_query() {
    assert_eq!(page_url("/posts", 3), "/posts?page=3");
}

#[test]
fn page_url_with_existing_query_base() {
    assert_eq!(
        page_url("/search?q=rust", 2),
        "/search?q=rust?page=2"
    );
}

// --- validate_comment_fields ---

#[test]
fn validation_all_valid() {
    let result = validate_comment_fields("Alice", "alice@example.com", "Great post!");
    assert!(result.is_valid());
    assert!(result.name_error.is_none());
    assert!(result.email_error.is_none());
    assert!(result.text_error.is_none());
}

#[test]
fn validation_empty_name() {
    let result = validate_comment_fields("", "alice@example.com", "Great post!");
    assert!(!result.is_valid());
    assert!(result.name_error.is_some());
    assert!(result.email_error.is_none());
}

#[test]
fn validation_whitespace_only_name() {
    let result = validate_comment_fields("   ", "alice@example.com", "Great post!");
    assert!(!result.is_valid());
    assert!(result.name_error.is_some());
}

#[test]
fn validation_name_too_long() {
    let long_name = "a".repeat(MAX_NAME_LEN + 1);
    let result = validate_comment_fields(&long_name, "alice@example.com", "Great post!");
    assert!(!result.is_valid());
    assert!(result.name_error.unwrap().contains("at most"));
}

#[test]
fn validation_empty_email() {
    let result = validate_comment_fields("Alice", "", "Great post!");
    assert!(!result.is_valid());
    assert!(result.email_error.is_some());
}

#[test]
fn validation_invalid_email_no_at() {
    let result = validate_comment_fields("Alice", "alice.example.com", "Great post!");
    assert!(!result.is_valid());
    assert!(result.email_error.unwrap().contains("valid email"));
}

#[test]
fn validation_invalid_email_no_dot() {
    let result = validate_comment_fields("Alice", "alice@example", "Great post!");
    assert!(!result.is_valid());
    assert!(result.email_error.unwrap().contains("valid email"));
}

#[test]
fn validation_email_too_long() {
    let long_email = format!("a@{}.com", "b".repeat(MAX_EMAIL_LEN));
    let result = validate_comment_fields("Alice", &long_email, "Great post!");
    assert!(!result.is_valid());
    assert!(result.email_error.unwrap().contains("at most"));
}

#[test]
fn validation_empty_text() {
    let result = validate_comment_fields("Alice", "alice@example.com", "");
    assert!(!result.is_valid());
    assert!(result.text_error.is_some());
}

#[test]
fn validation_text_too_long() {
    let long_text = "x".repeat(MAX_TEXT_LEN + 1);
    let result = validate_comment_fields("Alice", "alice@example.com", &long_text);
    assert!(!result.is_valid());
    assert!(result.text_error.unwrap().contains("at most"));
}

#[test]
fn validation_all_fields_invalid() {
    let result = validate_comment_fields("", "", "");
    assert!(!result.is_valid());
    assert!(result.name_error.is_some());
    assert!(result.email_error.is_some());
    assert!(result.text_error.is_some());
}

// --- tag_font_size ---

#[test]
fn tag_font_size_at_zero_weight() {
    let size = tag_font_size(0.0);
    assert!((size - MIN_FONT_SIZE_REM).abs() < f64::EPSILON);
}

#[test]
fn tag_font_size_at_full_weight() {
    let size = tag_font_size(1.0);
    assert!((size - MAX_FONT_SIZE_REM).abs() < f64::EPSILON);
}

#[test]
fn tag_font_size_at_half_weight() {
    let size = tag_font_size(0.5);
    let expected = (MIN_FONT_SIZE_REM + MAX_FONT_SIZE_REM) / 2.0;
    assert!((size - expected).abs() < f64::EPSILON);
}

// --- format_experience_date ---

#[test]
fn format_experience_date_valid() {
    assert_eq!(format_experience_date("2020-01-15"), "January 2020");
}

#[test]
fn format_experience_date_december() {
    assert_eq!(format_experience_date("2023-12-01"), "December 2023");
}

#[test]
fn format_experience_date_invalid_returns_original() {
    assert_eq!(format_experience_date("not-a-date"), "not-a-date");
}

#[test]
fn format_experience_date_empty_returns_empty() {
    assert_eq!(format_experience_date(""), "");
}

// --- validate_contact_fields ---

#[test]
fn contact_validation_all_valid() {
    let result = validate_contact("John", "john@example.com", "Hello there", "This is a test message");
    assert!(result.is_ok());
}

#[test]
fn contact_validation_empty_name() {
    let result = validate_contact("", "john@example.com", "Hello there", "This is a test message");
    assert!(result.is_err());
    assert!(result.unwrap_err().contains_key("name"));
}

#[test]
fn contact_validation_name_too_long() {
    let long_name = "a".repeat(MAX_CONTACT_NAME_LEN + 1);
    let result = validate_contact(&long_name, "john@example.com", "Hello there", "This is a test message");
    assert!(result.is_err());
    assert!(result.unwrap_err()["name"].contains("at most"));
}

#[test]
fn contact_validation_empty_email() {
    let result = validate_contact("John", "", "Hello there", "This is a test message");
    assert!(result.is_err());
    assert!(result.unwrap_err().contains_key("email"));
}

#[test]
fn contact_validation_invalid_email() {
    let result = validate_contact("John", "not-an-email", "Hello there", "This is a test message");
    assert!(result.is_err());
    assert!(result.unwrap_err()["email"].contains("Invalid"));
}

#[test]
fn contact_validation_email_too_long() {
    let long_email = format!("a@{}.com", "b".repeat(MAX_CONTACT_EMAIL_LEN));
    let result = validate_contact("John", &long_email, "Hello there", "This is a test message");
    assert!(result.is_err());
    assert!(result.unwrap_err()["email"].contains("at most"));
}

#[test]
fn contact_validation_empty_subject() {
    let result = validate_contact("John", "john@example.com", "", "This is a test message");
    assert!(result.is_err());
    assert!(result.unwrap_err().contains_key("subject"));
}

#[test]
fn contact_validation_subject_too_short() {
    let result = validate_contact("John", "john@example.com", "ab", "This is a test message");
    assert!(result.is_err());
    assert!(result.unwrap_err()["subject"].contains("at least"));
}

#[test]
fn contact_validation_subject_too_long() {
    let long_subject = "a".repeat(MAX_CONTACT_SUBJECT_LEN + 1);
    let result = validate_contact("John", "john@example.com", &long_subject, "This is a test message");
    assert!(result.is_err());
    assert!(result.unwrap_err()["subject"].contains("at most"));
}

#[test]
fn contact_validation_empty_message() {
    let result = validate_contact("John", "john@example.com", "Hello there", "");
    assert!(result.is_err());
    assert!(result.unwrap_err().contains_key("message"));
}

#[test]
fn contact_validation_message_too_short() {
    let result = validate_contact("John", "john@example.com", "Hello there", "short");
    assert!(result.is_err());
    assert!(result.unwrap_err()["message"].contains("at least"));
}

#[test]
fn contact_validation_message_too_long() {
    let long_message = "a".repeat(MAX_CONTACT_MESSAGE_LEN + 1);
    let result = validate_contact("John", "john@example.com", "Hello there", &long_message);
    assert!(result.is_err());
    assert!(result.unwrap_err()["message"].contains("at most"));
}

#[test]
fn contact_validation_all_fields_invalid() {
    let result = validate_contact("", "", "", "");
    assert!(result.is_err());
    let errors = result.unwrap_err();
    assert!(errors.contains_key("name"));
    assert!(errors.contains_key("email"));
    assert!(errors.contains_key("subject"));
    assert!(errors.contains_key("message"));
}

#[test]
fn contact_validation_trims_whitespace() {
    let result = validate_contact("  John  ", "  john@example.com  ", "  Hello there  ", "  This is a test message  ");
    assert!(result.is_ok());
}

// --- strip_html_tags ---

#[test]
fn strip_html_tags_removes_simple_tags() {
    assert_eq!(strip_html_tags("<p>Hello <b>world</b></p>"), "Hello world");
}

#[test]
fn strip_html_tags_returns_plain_text_as_is() {
    assert_eq!(strip_html_tags("no tags here"), "no tags here");
}

#[test]
fn strip_html_tags_handles_empty_string() {
    assert_eq!(strip_html_tags(""), "");
}

#[test]
fn strip_html_tags_handles_nested_tags() {
    assert_eq!(
        strip_html_tags("<div><p><strong>deep</strong></p></div>"),
        "deep"
    );
}

#[test]
fn strip_html_tags_preserves_whitespace_between_tags() {
    assert_eq!(strip_html_tags("<p>one</p> <p>two</p>"), "one two");
}

// --- estimate_reading_time ---

#[test]
fn reading_time_minimum_one_minute() {
    assert_eq!(estimate_reading_time("short"), 1);
}

#[test]
fn reading_time_empty_content() {
    assert_eq!(estimate_reading_time(""), 1);
}

#[test]
fn reading_time_strips_html() {
    let html = format!("<p>{}</p>", "word ".repeat(400));
    assert_eq!(estimate_reading_time(&html), 2);
}

#[test]
fn reading_time_exactly_200_words() {
    let text = "word ".repeat(200);
    assert_eq!(estimate_reading_time(&text), 1);
}

#[test]
fn reading_time_1000_words() {
    let text = "word ".repeat(1000);
    assert_eq!(estimate_reading_time(&text), 5);
}

// --- slugify ---

#[test]
fn slugify_simple_text() {
    assert_eq!(slugify("Hello World"), "hello-world");
}

#[test]
fn slugify_special_characters() {
    assert_eq!(slugify("What's New?"), "whats-new");
}

#[test]
fn slugify_multiple_spaces_and_dashes() {
    assert_eq!(slugify("hello   world--foo"), "hello-world-foo");
}

#[test]
fn slugify_numbers_preserved() {
    assert_eq!(slugify("Chapter 3 Overview"), "chapter-3-overview");
}

#[test]
fn slugify_uppercase_lowered() {
    assert_eq!(slugify("ALL CAPS TITLE"), "all-caps-title");
}

#[test]
fn slugify_underscores_become_dashes() {
    assert_eq!(slugify("hello_world"), "hello-world");
}

// --- extract_headings ---

#[test]
fn extract_headings_returns_empty_for_few_headings() {
    let html = "<h2>One</h2><h2>Two</h2>";
    assert!(extract_headings(html).is_empty());
}

#[test]
fn extract_headings_returns_entries_for_three_or_more() {
    let html = "<h2>First</h2><h2>Second</h2><h3>Third</h3>";
    let entries = extract_headings(html);
    assert_eq!(entries.len(), 3);
    assert_eq!(
        entries[0],
        TocEntry { id: "first".into(), text: "First".into(), level: 2 }
    );
    assert_eq!(entries[2].level, 3);
}

#[test]
fn extract_headings_strips_inner_html() {
    let html = "<h2><strong>Bold</strong> Title</h2><h2>Two</h2><h2>Three</h2>";
    let entries = extract_headings(html);
    assert_eq!(entries[0].text, "Bold Title");
}

#[test]
fn extract_headings_ignores_h1_and_h5() {
    let html = "<h1>Top</h1><h5>Small</h5><h2>A</h2><h2>B</h2><h2>C</h2>";
    let entries = extract_headings(html);
    assert_eq!(entries.len(), 3);
    assert_eq!(entries[0].text, "A");
}

#[test]
fn extract_headings_mixed_levels() {
    let html = "<h2>Intro</h2><h3>Sub</h3><h4>Detail</h4>";
    let entries = extract_headings(html);
    assert_eq!(entries.len(), 3);
    assert_eq!(entries[0].level, 2);
    assert_eq!(entries[1].level, 3);
    assert_eq!(entries[2].level, 4);
}

// --- inject_heading_ids ---

#[test]
fn inject_heading_ids_adds_ids() {
    let html = "<h2>Hello World</h2>";
    assert_eq!(
        inject_heading_ids(html),
        "<h2 id=\"hello-world\">Hello World</h2>"
    );
}

#[test]
fn inject_heading_ids_preserves_existing_id() {
    let html = "<h2 id=\"custom\">Title</h2>";
    assert_eq!(inject_heading_ids(html), html);
}

#[test]
fn inject_heading_ids_leaves_h1_alone() {
    let html = "<h1>Top Level</h1>";
    assert_eq!(inject_heading_ids(html), html);
}

#[test]
fn inject_heading_ids_handles_h3_and_h4() {
    let html = "<h3>Sub</h3><h4>Detail</h4>";
    assert_eq!(
        inject_heading_ids(html),
        "<h3 id=\"sub\">Sub</h3><h4 id=\"detail\">Detail</h4>"
    );
}

#[test]
fn inject_heading_ids_preserves_surrounding_content() {
    let html = "<p>Before</p><h2>Title</h2><p>After</p>";
    assert_eq!(
        inject_heading_ids(html),
        "<p>Before</p><h2 id=\"title\">Title</h2><p>After</p>"
    );
}
