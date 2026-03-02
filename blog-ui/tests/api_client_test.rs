#![cfg(feature = "ssr")]

use blog_ui::api::client::{ApiError, BlogApiClient};
use blog_ui::api::types::*;
use wiremock::matchers::{body_json, method, path, query_param};
use wiremock::{Mock, MockServer, ResponseTemplate};

async fn setup() -> (MockServer, BlogApiClient) {
    let server = MockServer::start().await;
    let client = BlogApiClient::new(server.uri());
    (server, client)
}

// --- Success paths ---

#[tokio::test]
async fn get_posts_returns_paginated_list() {
    let (server, client) = setup().await;

    let body = serde_json::json!({
        "items": [{
            "id": 1,
            "name": "Test Post",
            "short_text": "Excerpt",
            "created_at": "2024-01-15T10:00:00+00:00",
            "tags": [{"id": 1, "name": "Rust", "slug": "rust"}]
        }],
        "total": 1
    });

    Mock::given(method("GET"))
        .and(path("/v1/posts"))
        .and(query_param("limit", "10"))
        .and(query_param("offset", "0"))
        .respond_with(ResponseTemplate::new(200).set_body_json(&body))
        .mount(&server)
        .await;

    let result = client.get_posts(10, 0, None).await.unwrap();
    assert_eq!(result.total, 1);
    assert_eq!(result.items[0].name, "Test Post");
    assert_eq!(result.items[0].tags[0].slug, "rust");
}

#[tokio::test]
async fn get_posts_with_tag_filter() {
    let (server, client) = setup().await;

    let body = serde_json::json!({
        "items": [{
            "id": 2,
            "name": "Rust Post",
            "short_text": "About Rust",
            "created_at": "2024-02-01T12:00:00+00:00",
            "tags": [{"id": 1, "name": "Rust", "slug": "rust"}]
        }],
        "total": 1
    });

    Mock::given(method("GET"))
        .and(path("/v1/posts"))
        .and(query_param("tag", "rust"))
        .respond_with(ResponseTemplate::new(200).set_body_json(&body))
        .mount(&server)
        .await;

    let result = client.get_posts(10, 0, Some("rust")).await.unwrap();
    assert_eq!(result.total, 1);
    assert_eq!(result.items[0].name, "Rust Post");
}

#[tokio::test]
async fn search_posts_sends_query() {
    let (server, client) = setup().await;

    let body = serde_json::json!({
        "items": [{
            "id": 3,
            "name": "Search Result",
            "short_text": "Found it",
            "created_at": "2024-03-01T09:00:00+00:00",
            "tags": []
        }],
        "total": 1
    });

    Mock::given(method("GET"))
        .and(path("/v1/posts/search"))
        .and(query_param("q", "test"))
        .and(query_param("limit", "10"))
        .and(query_param("offset", "0"))
        .respond_with(ResponseTemplate::new(200).set_body_json(&body))
        .mount(&server)
        .await;

    let result = client.search_posts("test", 10, 0).await.unwrap();
    assert_eq!(result.total, 1);
    assert_eq!(result.items[0].name, "Search Result");
}

#[tokio::test]
async fn get_recent_posts_with_count() {
    let (server, client) = setup().await;

    let body = serde_json::json!([
        {
            "id": 1,
            "name": "Recent Post",
            "short_text": "Latest",
            "created_at": "2024-04-01T10:00:00+00:00",
            "tags": []
        }
    ]);

    Mock::given(method("GET"))
        .and(path("/v1/posts/recent"))
        .and(query_param("count", "5"))
        .respond_with(ResponseTemplate::new(200).set_body_json(&body))
        .mount(&server)
        .await;

    let result = client.get_recent_posts(5).await.unwrap();
    assert_eq!(result.len(), 1);
    assert_eq!(result[0].name, "Recent Post");
}

#[tokio::test]
async fn get_post_by_id() {
    let (server, client) = setup().await;

    let body = serde_json::json!({
        "name": "Full Post",
        "text": "<p>Body content</p>",
        "created_at": "2024-01-15T10:00:00+00:00",
        "tags": [{"id": 1, "name": "Rust", "slug": "rust"}]
    });

    Mock::given(method("GET"))
        .and(path("/v1/posts/1"))
        .respond_with(ResponseTemplate::new(200).set_body_json(&body))
        .mount(&server)
        .await;

    let result = client.get_post(1).await.unwrap();
    assert_eq!(result.name, "Full Post");
    assert_eq!(result.text, "<p>Body content</p>");
}

#[tokio::test]
async fn increment_view_returns_ok() {
    let (server, client) = setup().await;

    Mock::given(method("POST"))
        .and(path("/v1/posts/1/view"))
        .respond_with(ResponseTemplate::new(204))
        .mount(&server)
        .await;

    let result = client.increment_view(1).await;
    assert!(result.is_ok());
}

#[tokio::test]
async fn get_comments_with_nested_replies() {
    let (server, client) = setup().await;

    let body = serde_json::json!({
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
        "total": 2
    });

    Mock::given(method("GET"))
        .and(path("/v1/posts/1/comments"))
        .respond_with(ResponseTemplate::new(200).set_body_json(&body))
        .mount(&server)
        .await;

    let result = client.get_comments(1).await.unwrap();
    assert_eq!(result.total, 2);
    assert_eq!(result.comments[0].name, "Alice");
    assert_eq!(result.comments[0].replies[0].name, "Bob");
}

#[tokio::test]
async fn create_comment_sends_request_body() {
    let (server, client) = setup().await;

    let response_body = serde_json::json!({
        "id": 10,
        "name": "Alice",
        "text": "Nice post!",
        "rating": 0,
        "created_at": "2024-05-01T10:00:00+00:00",
        "replies": []
    });

    let req_body = CreateCommentRequest {
        name: "Alice".to_string(),
        email: "alice@example.com".to_string(),
        text: "Nice post!".to_string(),
        parent_id: None,
    };

    Mock::given(method("POST"))
        .and(path("/v1/posts/1/comments"))
        .and(body_json(&req_body))
        .respond_with(ResponseTemplate::new(201).set_body_json(&response_body))
        .mount(&server)
        .await;

    let req = CreateCommentRequest {
        name: "Alice".to_string(),
        email: "alice@example.com".to_string(),
        text: "Nice post!".to_string(),
        parent_id: None,
    };

    let result = client.create_comment(1, req).await.unwrap();
    assert_eq!(result.id, 10);
    assert_eq!(result.name, "Alice");
}

#[tokio::test]
async fn rate_comment_sends_upvote() {
    let (server, client) = setup().await;

    Mock::given(method("POST"))
        .and(path("/v1/comments/1/rate"))
        .and(body_json(&serde_json::json!({"is_upvote": true})))
        .respond_with(ResponseTemplate::new(204))
        .mount(&server)
        .await;

    let result = client.rate_comment(1, true).await;
    assert!(result.is_ok());
}

#[tokio::test]
async fn get_tags_returns_list() {
    let (server, client) = setup().await;

    let body = serde_json::json!({
        "items": [{"id": 1, "name": "Rust", "slug": "rust", "post_count": 5}],
        "total": 1
    });

    Mock::given(method("GET"))
        .and(path("/v1/tags"))
        .respond_with(ResponseTemplate::new(200).set_body_json(&body))
        .mount(&server)
        .await;

    let result = client.get_tags().await.unwrap();
    assert_eq!(result.total, 1);
    assert_eq!(result.items[0].name, "Rust");
    assert_eq!(result.items[0].post_count, 5);
}

#[tokio::test]
async fn get_tag_cloud_returns_weighted_tags() {
    let (server, client) = setup().await;

    let body = serde_json::json!({
        "tags": [
            {"name": "Rust", "slug": "rust", "count": 10, "weight": 1.0},
            {"name": "Scala", "slug": "scala", "count": 3, "weight": 0.3}
        ]
    });

    Mock::given(method("GET"))
        .and(path("/v1/tags/cloud"))
        .respond_with(ResponseTemplate::new(200).set_body_json(&body))
        .mount(&server)
        .await;

    let result = client.get_tag_cloud().await.unwrap();
    assert_eq!(result.tags.len(), 2);
    assert_eq!(result.tags[0].count, 10);
}

#[tokio::test]
async fn get_pages_returns_list() {
    let (server, client) = setup().await;

    let body = serde_json::json!({
        "items": [{"url": "about", "title": "About Me"}],
        "total": 1
    });

    Mock::given(method("GET"))
        .and(path("/v1/pages"))
        .respond_with(ResponseTemplate::new(200).set_body_json(&body))
        .mount(&server)
        .await;

    let result = client.get_pages().await.unwrap();
    assert_eq!(result.total, 1);
    assert_eq!(result.items[0].url, "about");
}

#[tokio::test]
async fn get_page_by_url() {
    let (server, client) = setup().await;

    let body = serde_json::json!({
        "id": 1,
        "url": "about",
        "title": "About Me",
        "content": "<p>About page content</p>",
        "created_at": "2024-01-10T08:00:00+00:00"
    });

    Mock::given(method("GET"))
        .and(path("/v1/pages/about"))
        .respond_with(ResponseTemplate::new(200).set_body_json(&body))
        .mount(&server)
        .await;

    let result = client.get_page("about").await.unwrap();
    assert_eq!(result.title, "About Me");
    assert_eq!(result.url, "about");
}

// --- Error handling ---

#[tokio::test]
async fn not_found_returns_api_error() {
    let (server, client) = setup().await;

    Mock::given(method("GET"))
        .and(path("/v1/posts/999"))
        .respond_with(ResponseTemplate::new(404).set_body_string("Post not found"))
        .mount(&server)
        .await;

    let result = client.get_post(999).await;
    assert!(result.is_err());
    match result.unwrap_err() {
        ApiError::NotFound(msg) => assert_eq!(msg, "Post not found"),
        other => panic!("Expected NotFound, got {:?}", other),
    }
}

#[tokio::test]
async fn validation_error_returns_api_error() {
    let (server, client) = setup().await;

    Mock::given(method("POST"))
        .and(path("/v1/posts/1/comments"))
        .respond_with(ResponseTemplate::new(422).set_body_string("Name is required"))
        .mount(&server)
        .await;

    let req = CreateCommentRequest {
        name: "".to_string(),
        email: "test@test.com".to_string(),
        text: "Hello".to_string(),
        parent_id: None,
    };

    let result = client.create_comment(1, req).await;
    assert!(result.is_err());
    match result.unwrap_err() {
        ApiError::Validation(msg) => assert_eq!(msg, "Name is required"),
        other => panic!("Expected Validation, got {:?}", other),
    }
}

#[tokio::test]
async fn server_error_returns_api_error() {
    let (server, client) = setup().await;

    Mock::given(method("GET"))
        .and(path("/v1/tags"))
        .respond_with(ResponseTemplate::new(500).set_body_string("Internal server error"))
        .mount(&server)
        .await;

    let result = client.get_tags().await;
    assert!(result.is_err());
    match result.unwrap_err() {
        ApiError::Server(msg) => assert_eq!(msg, "Internal server error"),
        other => panic!("Expected Server, got {:?}", other),
    }
}

// --- Skills ---

#[tokio::test]
async fn get_skills_returns_categories() {
    let (server, client) = setup().await;

    let body = serde_json::json!([{
        "category": "Backend",
        "skills": [
            {"id": 1, "name": "Scala", "slug": "scala", "category": "Backend", "proficiency": 90, "icon": null}
        ]
    }]);

    Mock::given(method("GET"))
        .and(path("/v1/skills"))
        .respond_with(ResponseTemplate::new(200).set_body_json(&body))
        .mount(&server)
        .await;

    let result = client.get_skills().await.unwrap();
    assert_eq!(result.len(), 1);
    assert_eq!(result[0].category, "Backend");
    assert_eq!(result[0].skills[0].name, "Scala");
    assert_eq!(result[0].skills[0].proficiency, 90);
}

#[tokio::test]
async fn get_skills_empty() {
    let (server, client) = setup().await;

    Mock::given(method("GET"))
        .and(path("/v1/skills"))
        .respond_with(ResponseTemplate::new(200).set_body_json(&serde_json::json!([])))
        .mount(&server)
        .await;

    let result = client.get_skills().await.unwrap();
    assert!(result.is_empty());
}

// --- Experiences ---

#[tokio::test]
async fn get_experiences_returns_list() {
    let (server, client) = setup().await;

    let body = serde_json::json!([{
        "id": 1,
        "company": "Acme Corp",
        "position": "Engineer",
        "description": "Building things",
        "start_date": "2020-01-01",
        "end_date": "2023-06-15",
        "location": "Remote",
        "company_url": "https://acme.com"
    }]);

    Mock::given(method("GET"))
        .and(path("/v1/experiences"))
        .respond_with(ResponseTemplate::new(200).set_body_json(&body))
        .mount(&server)
        .await;

    let result = client.get_experiences().await.unwrap();
    assert_eq!(result.len(), 1);
    assert_eq!(result[0].company, "Acme Corp");
    assert_eq!(result[0].end_date, Some("2023-06-15".to_string()));
}

#[tokio::test]
async fn get_experiences_with_null_end_date() {
    let (server, client) = setup().await;

    let body = serde_json::json!([{
        "id": 2,
        "company": "Current Job",
        "position": "CTO",
        "description": "Leading",
        "start_date": "2023-07-01",
        "end_date": null,
        "location": null,
        "company_url": null
    }]);

    Mock::given(method("GET"))
        .and(path("/v1/experiences"))
        .respond_with(ResponseTemplate::new(200).set_body_json(&body))
        .mount(&server)
        .await;

    let result = client.get_experiences().await.unwrap();
    assert_eq!(result.len(), 1);
    assert!(result[0].end_date.is_none());
}

// --- Social Links ---

#[tokio::test]
async fn get_social_links_returns_list() {
    let (server, client) = setup().await;

    let body = serde_json::json!([
        {"id": 1, "platform": "github", "url": "https://github.com/user", "label": "GitHub", "icon": "gh-icon"},
        {"id": 2, "platform": "linkedin", "url": "https://linkedin.com/in/user", "label": null, "icon": null}
    ]);

    Mock::given(method("GET"))
        .and(path("/v1/social-links"))
        .respond_with(ResponseTemplate::new(200).set_body_json(&body))
        .mount(&server)
        .await;

    let result = client.get_social_links().await.unwrap();
    assert_eq!(result.len(), 2);
    assert_eq!(result[0].platform, "github");
    assert_eq!(result[0].label, Some("GitHub".to_string()));
    assert!(result[1].label.is_none());
}

// --- Contact ---

#[tokio::test]
async fn submit_contact_success() {
    let (server, client) = setup().await;

    let req = CreateContactRequest {
        name: "John".to_string(),
        email: "john@example.com".to_string(),
        subject: "Hello".to_string(),
        message: "Test message".to_string(),
        website: None,
    };

    Mock::given(method("POST"))
        .and(path("/v1/contact"))
        .respond_with(
            ResponseTemplate::new(200)
                .set_body_json(&serde_json::json!({"message": "Thank you for your message!"})),
        )
        .mount(&server)
        .await;

    let result = client.submit_contact(&req).await.unwrap();
    assert_eq!(result.message, "Thank you for your message!");
}

#[tokio::test]
async fn submit_contact_rate_limited() {
    let (server, client) = setup().await;

    let req = CreateContactRequest {
        name: "John".to_string(),
        email: "john@example.com".to_string(),
        subject: "Hello".to_string(),
        message: "Test message".to_string(),
        website: None,
    };

    Mock::given(method("POST"))
        .and(path("/v1/contact"))
        .respond_with(ResponseTemplate::new(429).set_body_string("Too many requests"))
        .mount(&server)
        .await;

    let result = client.submit_contact(&req).await;
    assert!(result.is_err());
    match result.unwrap_err() {
        ApiError::Server(msg) => assert_eq!(msg, "Too many requests"),
        other => panic!("Expected Server error for 429, got {:?}", other),
    }
}

// --- About ---

#[tokio::test]
async fn get_about_returns_full_result() {
    let (server, client) = setup().await;

    let body = serde_json::json!({
        "profile": {
            "name": "John",
            "title": "Engineer",
            "photo_url": "https://example.com/photo.jpg",
            "resume_url": "https://example.com/resume.pdf",
            "bio": "About me"
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
    });

    Mock::given(method("GET"))
        .and(path("/v1/about"))
        .respond_with(ResponseTemplate::new(200).set_body_json(&body))
        .mount(&server)
        .await;

    let result = client.get_about().await.unwrap();
    assert_eq!(result.profile.name, "John");
    assert_eq!(result.skills.len(), 1);
    assert_eq!(result.experiences.len(), 1);
    assert_eq!(result.social_links.len(), 1);
}
