use serde::{Deserialize, Serialize};

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct ListItemsResult<T> {
    pub items: Vec<T>,
    pub total: i32,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct ListPostResult {
    pub id: i32,
    pub name: String,
    pub short_text: String,
    pub created_at: String,
    pub tags: Vec<TagResult>,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct PostResult {
    pub name: String,
    pub text: String,
    pub created_at: String,
    pub tags: Vec<TagResult>,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct TagResult {
    pub id: i32,
    pub name: String,
    pub slug: String,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct TagWithCountResult {
    pub id: i32,
    pub name: String,
    pub slug: String,
    pub post_count: i32,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct TagCloudItem {
    pub name: String,
    pub slug: String,
    pub count: i32,
    pub weight: f64,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct TagCloudResult {
    pub tags: Vec<TagCloudItem>,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct CommentResult {
    pub id: i32,
    pub name: String,
    pub text: String,
    pub rating: i32,
    pub created_at: String,
    pub replies: Vec<CommentResult>,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct CommentsListResult {
    pub comments: Vec<CommentResult>,
    pub total: i32,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct PageResult {
    pub id: i32,
    pub url: String,
    pub title: String,
    pub content: String,
    pub created_at: String,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct ListPageResult {
    pub url: String,
    pub title: String,
}

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct CreateCommentRequest {
    pub name: String,
    pub email: String,
    pub text: String,
    pub parent_id: Option<i32>,
}

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct RateCommentRequest {
    pub is_upvote: bool,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct ErrorResponse {
    pub code: String,
    pub message: String,
    pub details: Option<std::collections::HashMap<String, String>>,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct HealthResponse {
    pub status: String,
    pub database: String,
    pub timestamp: String,
}
