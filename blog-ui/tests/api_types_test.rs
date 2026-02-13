use blog_ui::api::types::*;

#[test]
fn deserialize_list_posts() {
    let json = r#"{
        "items": [{
            "id": 1,
            "name": "Test Post",
            "short_text": "A short excerpt",
            "created_at": "2024-01-15T10:00:00+00:00",
            "tags": [{"id": 1, "name": "Rust", "slug": "rust"}]
        }],
        "total": 1
    }"#;
    let result: ListItemsResult<ListPostResult> = serde_json::from_str(json).unwrap();
    assert_eq!(result.total, 1);
    assert_eq!(result.items.len(), 1);
    assert_eq!(result.items[0].id, 1);
    assert_eq!(result.items[0].short_text, "A short excerpt");
    assert_eq!(result.items[0].tags[0].slug, "rust");
}

#[test]
fn deserialize_list_posts_empty() {
    let json = r#"{"items": [], "total": 0}"#;
    let result: ListItemsResult<ListPostResult> = serde_json::from_str(json).unwrap();
    assert_eq!(result.total, 0);
    assert!(result.items.is_empty());
}

#[test]
fn deserialize_post_result() {
    let json = r#"{
        "name": "Full Post",
        "text": "<p>Post body HTML</p>",
        "created_at": "2024-01-15T10:00:00+00:00",
        "tags": [
            {"id": 1, "name": "Rust", "slug": "rust"},
            {"id": 2, "name": "Web", "slug": "web"}
        ]
    }"#;
    let result: PostResult = serde_json::from_str(json).unwrap();
    assert_eq!(result.name, "Full Post");
    assert_eq!(result.tags.len(), 2);
}

#[test]
fn deserialize_tag_with_count() {
    let json = r#"{
        "items": [{"id": 1, "name": "Rust", "slug": "rust", "post_count": 5}],
        "total": 1
    }"#;
    let result: ListItemsResult<TagWithCountResult> = serde_json::from_str(json).unwrap();
    assert_eq!(result.items[0].post_count, 5);
}

#[test]
fn deserialize_tag_cloud() {
    let json = r#"{
        "tags": [
            {"name": "Rust", "slug": "rust", "count": 10, "weight": 1.0},
            {"name": "Scala", "slug": "scala", "count": 3, "weight": 0.3}
        ]
    }"#;
    let result: TagCloudResult = serde_json::from_str(json).unwrap();
    assert_eq!(result.tags.len(), 2);
    assert_eq!(result.tags[0].count, 10);
    assert!((result.tags[1].weight - 0.3).abs() < f64::EPSILON);
}

#[test]
fn deserialize_comments_with_nested_replies() {
    let json = r#"{
        "comments": [{
            "id": 1,
            "name": "Alice",
            "text": "Great post!",
            "rating": 5,
            "created_at": "2024-01-16T12:00:00+00:00",
            "replies": [{
                "id": 2,
                "name": "Bob",
                "text": "Thanks!",
                "rating": 2,
                "created_at": "2024-01-16T13:00:00+00:00",
                "replies": []
            }]
        }],
        "total": 1
    }"#;
    let result: CommentsListResult = serde_json::from_str(json).unwrap();
    assert_eq!(result.total, 1);
    assert_eq!(result.comments[0].replies.len(), 1);
    assert_eq!(result.comments[0].replies[0].name, "Bob");
}

#[test]
fn deserialize_page_result() {
    let json = r#"{
        "id": 1,
        "url": "about",
        "title": "About Me",
        "content": "<p>About page content</p>",
        "created_at": "2024-01-10T08:00:00+00:00"
    }"#;
    let result: PageResult = serde_json::from_str(json).unwrap();
    assert_eq!(result.url, "about");
    assert_eq!(result.title, "About Me");
}

#[test]
fn deserialize_list_page_result() {
    let json = r#"{
        "items": [{"url": "about", "title": "About"}],
        "total": 1
    }"#;
    let result: ListItemsResult<ListPageResult> = serde_json::from_str(json).unwrap();
    assert_eq!(result.items[0].url, "about");
}

#[test]
fn deserialize_error_response_with_details() {
    let json = r#"{
        "code": "VALIDATION_ERROR",
        "message": "Invalid input",
        "details": {"name": "required"}
    }"#;
    let result: ErrorResponse = serde_json::from_str(json).unwrap();
    assert_eq!(result.code, "VALIDATION_ERROR");
    assert!(result.details.is_some());
    assert_eq!(result.details.unwrap().get("name").unwrap(), "required");
}

#[test]
fn deserialize_error_response_without_details() {
    let json = r#"{
        "code": "NOT_FOUND",
        "message": "Post not found",
        "details": null
    }"#;
    let result: ErrorResponse = serde_json::from_str(json).unwrap();
    assert_eq!(result.code, "NOT_FOUND");
    assert!(result.details.is_none());
}

#[test]
fn deserialize_health_response() {
    let json = r#"{
        "status": "ok",
        "database": "connected",
        "timestamp": "2024-01-15T10:00:00Z"
    }"#;
    let result: HealthResponse = serde_json::from_str(json).unwrap();
    assert_eq!(result.status, "ok");
}

#[test]
fn serialize_create_comment_request() {
    let req = CreateCommentRequest {
        name: "Alice".to_string(),
        email: "alice@example.com".to_string(),
        text: "Nice post!".to_string(),
        parent_id: Some(1),
    };
    let json = serde_json::to_value(&req).unwrap();
    assert_eq!(json["parent_id"], 1);
    assert_eq!(json["name"], "Alice");
}

#[test]
fn serialize_rate_comment_request() {
    let req = RateCommentRequest { is_upvote: true };
    let json = serde_json::to_value(&req).unwrap();
    assert_eq!(json["is_upvote"], true);
}
