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

// --- Skill types ---

#[test]
fn deserialize_skill_category_result() {
    let json = r#"[{
        "category": "Backend",
        "skills": [
            {"id": 1, "name": "Scala", "slug": "scala", "category": "Backend", "proficiency": 90, "icon": "scala-icon"},
            {"id": 2, "name": "Rust", "slug": "rust", "category": "Backend", "proficiency": 80, "icon": null}
        ]
    }]"#;
    let result: Vec<SkillCategoryResult> = serde_json::from_str(json).unwrap();
    assert_eq!(result.len(), 1);
    assert_eq!(result[0].category, "Backend");
    assert_eq!(result[0].skills.len(), 2);
    assert_eq!(result[0].skills[0].name, "Scala");
    assert_eq!(result[0].skills[0].proficiency, 90);
    assert_eq!(result[0].skills[0].icon, Some("scala-icon".to_string()));
    assert_eq!(result[0].skills[1].icon, None);
}

#[test]
fn deserialize_skill_category_result_empty() {
    let json = r#"[]"#;
    let result: Vec<SkillCategoryResult> = serde_json::from_str(json).unwrap();
    assert!(result.is_empty());
}

// --- Experience types ---

#[test]
fn deserialize_experience_result_with_end_date() {
    let json = r#"{
        "id": 1,
        "company": "Acme Corp",
        "position": "Engineer",
        "description": "Building things",
        "start_date": "2020-01-01",
        "end_date": "2023-06-15",
        "location": "Remote",
        "company_url": "https://acme.com"
    }"#;
    let result: ExperienceResult = serde_json::from_str(json).unwrap();
    assert_eq!(result.company, "Acme Corp");
    assert_eq!(result.position, "Engineer");
    assert_eq!(result.start_date, "2020-01-01");
    assert_eq!(result.end_date, Some("2023-06-15".to_string()));
    assert_eq!(result.location, Some("Remote".to_string()));
    assert_eq!(result.company_url, Some("https://acme.com".to_string()));
}

#[test]
fn deserialize_experience_result_without_end_date() {
    let json = r#"{
        "id": 2,
        "company": "Startup Inc",
        "position": "CTO",
        "description": "Leading tech",
        "start_date": "2023-07-01",
        "end_date": null,
        "location": null,
        "company_url": null
    }"#;
    let result: ExperienceResult = serde_json::from_str(json).unwrap();
    assert_eq!(result.company, "Startup Inc");
    assert!(result.end_date.is_none());
    assert!(result.location.is_none());
    assert!(result.company_url.is_none());
}

// --- SocialLink types ---

#[test]
fn deserialize_social_link_result() {
    let json = r#"[
        {"id": 1, "platform": "github", "url": "https://github.com/user", "label": "GitHub", "icon": "gh-icon"},
        {"id": 2, "platform": "linkedin", "url": "https://linkedin.com/in/user", "label": null, "icon": null}
    ]"#;
    let result: Vec<SocialLinkResult> = serde_json::from_str(json).unwrap();
    assert_eq!(result.len(), 2);
    assert_eq!(result[0].platform, "github");
    assert_eq!(result[0].label, Some("GitHub".to_string()));
    assert!(result[1].label.is_none());
}

// --- Contact types ---

#[test]
fn serialize_create_contact_request() {
    let req = CreateContactRequest {
        name: "John".to_string(),
        email: "john@example.com".to_string(),
        subject: "Hello".to_string(),
        message: "Test message".to_string(),
        website: None,
    };
    let json = serde_json::to_value(&req).unwrap();
    assert_eq!(json["name"], "John");
    assert_eq!(json["subject"], "Hello");
    assert!(json.get("website").is_none());
}

#[test]
fn serialize_create_contact_request_with_honeypot() {
    let req = CreateContactRequest {
        name: "John".to_string(),
        email: "john@example.com".to_string(),
        subject: "Hello".to_string(),
        message: "Test message".to_string(),
        website: Some("spam".to_string()),
    };
    let json = serde_json::to_value(&req).unwrap();
    assert_eq!(json["website"], "spam");
}

#[test]
fn deserialize_contact_response() {
    let json = r#"{"message": "Thank you for your message!"}"#;
    let result: ContactResponse = serde_json::from_str(json).unwrap();
    assert_eq!(result.message, "Thank you for your message!");
}

// --- About types ---

#[test]
fn deserialize_about_result() {
    let json = r#"{
        "profile": {
            "name": "John",
            "title": "Engineer",
            "photo_url": "https://example.com/photo.jpg",
            "resume_url": "https://example.com/resume.pdf",
            "bio": "About me text"
        },
        "skills": [{
            "category": "Backend",
            "skills": [{"id": 1, "name": "Scala", "slug": "scala", "category": "Backend", "proficiency": 90, "icon": null}]
        }],
        "experiences": [{
            "id": 1,
            "company": "Acme",
            "position": "Engineer",
            "description": "Work",
            "start_date": "2020-01-01",
            "end_date": null,
            "location": "Remote",
            "company_url": null
        }],
        "social_links": [
            {"id": 1, "platform": "github", "url": "https://github.com", "label": "GitHub", "icon": null}
        ]
    }"#;
    let result: AboutResult = serde_json::from_str(json).unwrap();
    assert_eq!(result.profile.name, "John");
    assert_eq!(result.profile.title, "Engineer");
    assert_eq!(result.profile.bio, "About me text");
    assert_eq!(result.skills.len(), 1);
    assert_eq!(result.experiences.len(), 1);
    assert_eq!(result.social_links.len(), 1);
}

#[test]
fn deserialize_about_result_empty_sections() {
    let json = r#"{
        "profile": {
            "name": "",
            "title": "",
            "photo_url": "",
            "resume_url": "",
            "bio": ""
        },
        "skills": [],
        "experiences": [],
        "social_links": []
    }"#;
    let result: AboutResult = serde_json::from_str(json).unwrap();
    assert!(result.skills.is_empty());
    assert!(result.experiences.is_empty());
    assert!(result.social_links.is_empty());
}
