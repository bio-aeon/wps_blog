use blog_ui::components::about::experience_item::format_experience_date;
use blog_ui::components::comment::comment_form::{
    validate_comment_fields, MAX_EMAIL_LEN, MAX_NAME_LEN, MAX_TEXT_LEN,
};
use blog_ui::components::common::pagination::{calculate_pagination, page_url};
use blog_ui::components::contact::contact_form::{
    validate_contact_fields as validate_contact, MAX_CONTACT_EMAIL_LEN, MAX_CONTACT_MESSAGE_LEN,
    MAX_CONTACT_NAME_LEN, MAX_CONTACT_SUBJECT_LEN, MIN_CONTACT_MESSAGE_LEN,
    MIN_CONTACT_SUBJECT_LEN,
};
use blog_ui::components::post::format_date;
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
